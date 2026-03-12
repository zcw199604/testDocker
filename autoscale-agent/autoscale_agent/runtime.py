"""Docker runtime operations for managed replicas."""

from __future__ import annotations

from dataclasses import dataclass, field
from datetime import datetime, timezone
import json
import logging
import time
from typing import Any
from urllib import error, request

import docker
from docker.models.containers import Container

from autoscale_agent.compose_loader import ServiceTemplate
from autoscale_agent.config import AgentSettings

LOGGER = logging.getLogger(__name__)


@dataclass(slots=True)
class Replica:
    """Container runtime view used by the autoscale controller."""

    container: Container
    name: str
    role: str
    status: str
    health_status: str
    addresses: list[str]
    created_at: datetime
    cpu_percent: float | None = None
    memory_percent: float | None = None
    app_metrics: dict[str, float] = field(default_factory=dict)

    @property
    def is_running(self) -> bool:
        """Returns whether the container is currently running."""

        return self.status == "running"

    @property
    def is_healthy(self) -> bool:
        """Returns whether the container is ready to receive traffic."""

        if not self.is_running:
            return False
        return self.health_status in {"healthy", "none", ""}


class DockerRuntime:
    """Discovers seed and managed replicas and operates on Docker directly."""

    def __init__(self, settings: AgentSettings, template: ServiceTemplate) -> None:
        self._settings = settings
        self._template = template
        self._client = docker.from_env()

    @property
    def client(self) -> docker.DockerClient:
        """Returns the Docker client used by the runtime."""

        return self._client

    def list_replicas(self) -> list[Replica]:
        """Lists seed and managed replicas for the target service."""

        containers = self._client.containers.list(
            all=True,
            filters={
                "label": [
                    f"com.docker.compose.project={self._template.project_name}",
                    f"com.docker.compose.service={self._template.service_name}",
                ]
            },
        )
        replicas = [self._build_replica(container) for container in containers]
        return sorted(replicas, key=lambda item: (item.created_at, item.name))

    def populate_runtime_metrics(self, replica: Replica) -> Replica:
        """Populates CPU, memory, and application metrics for a replica."""

        if not replica.is_running:
            return replica

        stats = replica.container.stats(stream=False)
        replica.cpu_percent = _calculate_cpu_percent(stats)
        replica.memory_percent = _calculate_memory_percent(stats)
        replica.app_metrics = self._read_app_metrics(replica.addresses)
        return replica

    def create_managed_replica(self, seed: Replica) -> Replica:
        """Creates a managed replica using the Compose-derived template."""

        labels = dict(self._template.labels)
        labels["com.docker.compose.project"] = self._template.project_name
        labels["com.docker.compose.service"] = self._template.service_name
        labels[self._settings.managed_role_label] = self._settings.managed_role_value

        name = f"{self._template.project_name}-{self._template.service_name}-managed-{time.time_ns()}"
        network_names = self._network_names_from_container(seed.container)
        create_kwargs: dict[str, Any] = {
            "image": self._template.image,
            "name": name,
            "detach": True,
            "environment": self._template.environment,
            "labels": labels,
            "hostname": name,
        }
        if self._template.command is not None:
            create_kwargs["command"] = self._template.command
        if self._template.entrypoint is not None:
            create_kwargs["entrypoint"] = self._template.entrypoint
        if self._template.working_dir is not None:
            create_kwargs["working_dir"] = self._template.working_dir
        if self._template.user is not None:
            create_kwargs["user"] = self._template.user
        if self._template.healthcheck is not None:
            create_kwargs["healthcheck"] = self._template.healthcheck
        if network_names:
            create_kwargs["network"] = network_names[0]

        container = self._client.containers.run(**create_kwargs)
        for network_name in network_names[1:]:
            self._client.networks.get(network_name).connect(container)
        LOGGER.info("Created managed replica %s", name)
        return self._build_replica(container)

    def wait_until_healthy(self, replica: Replica, timeout_seconds: int) -> bool:
        """Waits until the replica becomes healthy or times out."""

        deadline = time.monotonic() + timeout_seconds
        while time.monotonic() < deadline:
            replica = self._build_replica(replica.container)
            if replica.health_status == "healthy":
                return True
            if replica.health_status in {"none", ""} and self._probe_health(replica.addresses):
                return True
            if replica.status in {"exited", "dead"}:
                return False
            time.sleep(2)
        return False

    def wait_for_inflight_drain(self, replica: Replica, timeout_seconds: int) -> None:
        """Waits for the replica to finish in-flight requests or times out."""

        deadline = time.monotonic() + timeout_seconds
        while time.monotonic() < deadline:
            refreshed = self._build_replica(replica.container)
            if not refreshed.is_running:
                return

            metrics = self._read_app_metrics(refreshed.addresses)
            if metrics and int(metrics.get("inflightRequests", 0)) <= 0:
                LOGGER.info("Replica %s drained in-flight requests", refreshed.name)
                return
            time.sleep(2)

        LOGGER.info(
            "Replica %s drain window reached after %ss; continuing with shutdown",
            replica.name,
            timeout_seconds,
        )

    def stop_and_remove(self, replica: Replica) -> None:
        """Stops and removes a managed replica."""

        LOGGER.info("Stopping managed replica %s", replica.name)
        replica.container.stop(timeout=10)
        replica.container.remove(v=False)

    def remove_replica_immediately(self, replica: Replica) -> None:
        """Force-removes a replica, used after failed startup."""

        LOGGER.warning("Removing failed managed replica %s", replica.name)
        replica.container.remove(force=True, v=False)

    def _build_replica(self, container: Container) -> Replica:
        container.reload()
        attrs = container.attrs
        labels = attrs.get("Config", {}).get("Labels", {}) or {}
        role = (
            "managed"
            if labels.get(self._settings.managed_role_label) == self._settings.managed_role_value
            else "seed"
        )
        addresses = [
            network.get("IPAddress", "")
            for network in attrs.get("NetworkSettings", {}).get("Networks", {}).values()
            if network.get("IPAddress")
        ]
        created_raw = attrs.get("Created", "")
        created_at = datetime.fromisoformat(created_raw.replace("Z", "+00:00"))
        health_status = attrs.get("State", {}).get("Health", {}).get("Status", "none")
        return Replica(
            container=container,
            name=container.name,
            role=role,
            status=container.status,
            health_status=health_status,
            addresses=addresses,
            created_at=created_at.astimezone(timezone.utc),
        )

    def _network_names_from_container(self, container: Container) -> list[str]:
        networks = list(container.attrs.get("NetworkSettings", {}).get("Networks", {}).keys())
        return networks or [f"{self._template.project_name}_default"]

    def _probe_health(self, addresses: list[str]) -> bool:
        for address in addresses:
            url = f"http://{address}:{self._template.target_port}{self._template.health_path}"
            try:
                with request.urlopen(url, timeout=self._settings.http_timeout_seconds) as response:
                    if 200 <= response.status < 300:
                        return True
            except (error.URLError, TimeoutError):
                continue
        return False

    def _read_app_metrics(self, addresses: list[str]) -> dict[str, float]:
        for address in addresses:
            url = f"http://{address}:{self._template.target_port}{self._template.metrics_path}"
            try:
                with request.urlopen(url, timeout=self._settings.http_timeout_seconds) as response:
                    if response.status >= 400:
                        continue
                    payload = json.loads(response.read().decode("utf-8"))
                    flattened = _flatten_numeric_metrics(payload)
                    if flattened:
                        return flattened
            except (error.URLError, TimeoutError, json.JSONDecodeError) as exc:
                LOGGER.debug("Failed to read app metrics from %s: %s", url, exc)
        return {}


def _calculate_cpu_percent(stats: dict[str, Any]) -> float:
    cpu_stats = stats.get("cpu_stats", {})
    precpu_stats = stats.get("precpu_stats", {})
    current_total = cpu_stats.get("cpu_usage", {}).get("total_usage", 0)
    previous_total = precpu_stats.get("cpu_usage", {}).get("total_usage", 0)
    current_system = cpu_stats.get("system_cpu_usage", 0)
    previous_system = precpu_stats.get("system_cpu_usage", 0)
    cpu_delta = current_total - previous_total
    system_delta = current_system - previous_system
    cpu_count = len(cpu_stats.get("cpu_usage", {}).get("percpu_usage", []) or [1])
    if cpu_delta <= 0 or system_delta <= 0:
        return 0.0
    return (cpu_delta / system_delta) * cpu_count * 100.0


def _calculate_memory_percent(stats: dict[str, Any]) -> float:
    memory_stats = stats.get("memory_stats", {})
    usage = float(memory_stats.get("usage", 0.0))
    limit = float(memory_stats.get("limit", 0.0))
    if limit <= 0:
        return 0.0
    return (usage / limit) * 100.0


def _flatten_numeric_metrics(payload: Any, prefix: str = "") -> dict[str, float]:
    metrics: dict[str, float] = {}
    if isinstance(payload, dict):
        for key, value in payload.items():
            next_prefix = f"{prefix}.{key}" if prefix else str(key)
            metrics.update(_flatten_numeric_metrics(value, next_prefix))
    elif isinstance(payload, (int, float)) and not isinstance(payload, bool):
        metrics[prefix] = float(payload)
    return metrics
