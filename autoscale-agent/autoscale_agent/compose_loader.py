"""Compose template loading utilities."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import re
from typing import Any

import yaml


SERVICE_PORT_LABEL = "io.testdocker.autoscale.service-port"
HEALTH_PATH_LABEL = "io.testdocker.autoscale.health-path"
METRICS_PATH_LABEL = "io.testdocker.autoscale.metrics-path"
UPSTREAM_NAME_LABEL = "io.testdocker.autoscale.upstream-name"
SEED_HOST_LABEL = "io.testdocker.autoscale.seed-host"

_DURATION_FACTORS = {
    "ns": 1,
    "us": 1_000,
    "ms": 1_000_000,
    "s": 1_000_000_000,
    "m": 60 * 1_000_000_000,
    "h": 60 * 60 * 1_000_000_000,
}
_DURATION_PATTERN = re.compile(r"(\d+)(ns|us|ms|s|m|h)")


@dataclass(frozen=True, slots=True)
class ServiceTemplate:
    """Runtime template derived from the Compose service definition."""

    project_name: str
    service_name: str
    image: str
    environment: dict[str, str]
    labels: dict[str, str]
    healthcheck: dict[str, Any] | None
    command: str | list[str] | None
    entrypoint: str | list[str] | None
    working_dir: str | None
    user: str | None
    target_port: int
    health_path: str
    metrics_path: str
    upstream_name: str
    seed_host: str


def load_service_template(compose_path: Path, service_name: str) -> ServiceTemplate:
    """Loads one service definition from the Compose file."""

    with compose_path.open("r", encoding="utf-8") as handle:
        compose_data = yaml.safe_load(handle)

    services = compose_data.get("services", {})
    if service_name not in services:
        raise KeyError(f"Compose service '{service_name}' was not found.")

    service = services[service_name]
    labels = _normalize_key_values(service.get("labels"))
    environment = _normalize_key_values(service.get("environment"))
    project_name = compose_data.get("name") or labels.get("com.docker.compose.project") or compose_path.parent.name

    target_port = int(labels.get(SERVICE_PORT_LABEL, _read_target_port(service)))
    health_path = labels.get(HEALTH_PATH_LABEL, "/health")
    metrics_path = labels.get(METRICS_PATH_LABEL, "/metrics")
    upstream_name = labels.get(UPSTREAM_NAME_LABEL, "biz_service_upstream")
    seed_host = labels.get(SEED_HOST_LABEL, service_name)

    image = service.get("image")
    if not image:
        raise ValueError(f"Compose service '{service_name}' must define an image for autoscaling.")

    return ServiceTemplate(
        project_name=str(project_name),
        service_name=service_name,
        image=str(image),
        environment=environment,
        labels=labels,
        healthcheck=_parse_healthcheck(service.get("healthcheck")),
        command=service.get("command"),
        entrypoint=service.get("entrypoint"),
        working_dir=service.get("working_dir"),
        user=service.get("user"),
        target_port=target_port,
        health_path=health_path,
        metrics_path=metrics_path,
        upstream_name=upstream_name,
        seed_host=seed_host,
    )


def _normalize_key_values(raw: Any) -> dict[str, str]:
    if raw is None:
        return {}
    if isinstance(raw, dict):
        return {str(key): "" if value is None else str(value) for key, value in raw.items()}
    if isinstance(raw, list):
        result: dict[str, str] = {}
        for item in raw:
            key, _, value = str(item).partition("=")
            result[key] = value
        return result
    raise TypeError(f"Unsupported compose key/value mapping format: {type(raw)!r}")


def _read_target_port(service: dict[str, Any]) -> int:
    expose = service.get("expose") or []
    if expose:
        return _extract_port(expose[0])

    ports = service.get("ports") or []
    if ports:
        port = ports[0]
        if isinstance(port, dict):
            return int(port.get("target"))
        return _extract_port(port)
    raise ValueError("Service must expose an internal target port for nginx upstream generation.")


def _extract_port(value: Any) -> int:
    text = str(value)
    digits = re.findall(r"(\d+)", text)
    if not digits:
        raise ValueError(f"Unable to determine port from value: {value!r}")
    return int(digits[-1])


def _parse_healthcheck(healthcheck: dict[str, Any] | None) -> dict[str, Any] | None:
    if not healthcheck or healthcheck.get("disable"):
        return None

    result: dict[str, Any] = {}
    test = healthcheck.get("test")
    if test is not None:
        result["test"] = test if isinstance(test, list) else ["CMD-SHELL", str(test)]
    for key in ("interval", "timeout", "start_period"):
        if key in healthcheck:
            result[key] = _duration_to_nanoseconds(healthcheck[key])
    if "retries" in healthcheck:
        result["retries"] = int(healthcheck["retries"])
    return result or None


def _duration_to_nanoseconds(value: Any) -> int:
    if isinstance(value, (int, float)):
        return int(value)

    text = str(value).strip()
    if text.isdigit():
        return int(text)

    total = 0
    position = 0
    for match in _DURATION_PATTERN.finditer(text):
        if match.start() != position:
            raise ValueError(f"Unsupported duration format: {value!r}")
        total += int(match.group(1)) * _DURATION_FACTORS[match.group(2)]
        position = match.end()
    if position != len(text):
        raise ValueError(f"Unsupported duration format: {value!r}")
    return total
