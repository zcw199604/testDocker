"""Configuration loading for the autoscale agent."""

from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
import json
import os
from typing import Mapping


@dataclass(frozen=True, slots=True)
class AppMetricRule:
    """Thresholds for one application-level metric."""

    name: str
    scale_up_threshold: float
    scale_down_threshold: float
    aggregation: str = "max"


@dataclass(frozen=True, slots=True)
class AgentSettings:
    """Environment-backed runtime configuration."""

    compose_file: Path
    generated_dir: Path
    target_service: str
    nginx_service: str
    poll_interval_seconds: int
    min_replicas: int
    max_replicas: int
    scale_up_window: int
    scale_down_window: int
    cooldown_seconds: int
    drain_seconds: int
    startup_timeout_seconds: int
    http_timeout_seconds: float
    cpu_up_threshold: float
    cpu_down_threshold: float
    memory_up_threshold: float
    memory_down_threshold: float
    app_metric_rules: tuple[AppMetricRule, ...]
    dry_run: bool = False
    failure_freeze: bool = True
    managed_role_label: str = "io.testdocker.autoscale.role"
    managed_role_value: str = "managed"

    @property
    def upstream_file(self) -> Path:
        """Returns the generated upstream include path."""

        return self.generated_dir / "biz-service.upstream.inc"

    @classmethod
    def from_env(cls, env: Mapping[str, str] | None = None) -> "AgentSettings":
        """Builds settings from environment variables."""

        data = dict(os.environ if env is None else env)
        return cls(
            compose_file=Path(data.get("AUTOSCALE_COMPOSE_FILE", "/workspace/docker-compose.yml")),
            generated_dir=Path(data.get("AUTOSCALE_GENERATED_DIR", "/workspace/generated")),
            target_service=data.get("AUTOSCALE_TARGET_SERVICE", "biz-service"),
            nginx_service=data.get("AUTOSCALE_NGINX_SERVICE", "nginx"),
            poll_interval_seconds=_parse_int(data, "AUTOSCALE_POLL_INTERVAL_SECONDS", 15),
            min_replicas=_parse_int(data, "AUTOSCALE_MIN_REPLICAS", 1),
            max_replicas=_parse_int(data, "AUTOSCALE_MAX_REPLICAS", 4),
            scale_up_window=_parse_int(data, "AUTOSCALE_SCALE_UP_WINDOW", 2),
            scale_down_window=_parse_int(data, "AUTOSCALE_SCALE_DOWN_WINDOW", 3),
            cooldown_seconds=_parse_int(data, "AUTOSCALE_COOLDOWN_SECONDS", 60),
            drain_seconds=_parse_int(data, "AUTOSCALE_DRAIN_SECONDS", 20),
            startup_timeout_seconds=_parse_int(data, "AUTOSCALE_STARTUP_TIMEOUT_SECONDS", 180),
            http_timeout_seconds=_parse_float(data, "AUTOSCALE_HTTP_TIMEOUT_SECONDS", 2.5),
            cpu_up_threshold=_parse_float(data, "AUTOSCALE_CPU_UP_THRESHOLD", 70.0),
            cpu_down_threshold=_parse_float(data, "AUTOSCALE_CPU_DOWN_THRESHOLD", 25.0),
            memory_up_threshold=_parse_float(data, "AUTOSCALE_MEMORY_UP_THRESHOLD", 80.0),
            memory_down_threshold=_parse_float(data, "AUTOSCALE_MEMORY_DOWN_THRESHOLD", 35.0),
            app_metric_rules=_parse_app_rules(data.get("AUTOSCALE_APP_METRIC_RULES", "[]")),
            dry_run=_parse_bool(data, "AUTOSCALE_DRY_RUN", False),
            failure_freeze=_parse_bool(data, "AUTOSCALE_FAILURE_FREEZE", True),
        )


def _parse_int(env: Mapping[str, str], key: str, default: int) -> int:
    value = env.get(key)
    return default if value is None else int(value)


def _parse_float(env: Mapping[str, str], key: str, default: float) -> float:
    value = env.get(key)
    return default if value is None else float(value)


def _parse_bool(env: Mapping[str, str], key: str, default: bool) -> bool:
    value = env.get(key)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "on"}


def _parse_app_rules(raw: str) -> tuple[AppMetricRule, ...]:
    items = json.loads(raw)
    if not isinstance(items, list):
        raise ValueError("AUTOSCALE_APP_METRIC_RULES must be a JSON list.")

    rules: list[AppMetricRule] = []
    for item in items:
        if not isinstance(item, dict):
            raise ValueError("Each application metric rule must be a JSON object.")
        rules.append(
            AppMetricRule(
                name=str(item["name"]),
                scale_up_threshold=float(item["up"]),
                scale_down_threshold=float(item["down"]),
                aggregation=str(item.get("aggregation", "max")),
            )
        )
    return tuple(rules)
