"""Metric aggregation helpers for autoscaling decisions."""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Iterable, Protocol

from autoscale_agent.config import AppMetricRule


class ReplicaMetricsView(Protocol):
    """Small protocol used for metric aggregation."""

    cpu_percent: float | None
    memory_percent: float | None
    app_metrics: dict[str, float]


@dataclass(frozen=True, slots=True)
class AggregatedMetrics:
    """Aggregated metrics across active replicas."""

    cpu_percent: float | None
    memory_percent: float | None
    app_metrics: dict[str, float] = field(default_factory=dict)
    sample_size: int = 0


def aggregate_metrics(replicas: Iterable[ReplicaMetricsView], rules: tuple[AppMetricRule, ...]) -> AggregatedMetrics:
    """Aggregates CPU, memory, and application metrics across replicas."""

    replica_list = list(replicas)
    cpu_values = [replica.cpu_percent for replica in replica_list if replica.cpu_percent is not None]
    memory_values = [replica.memory_percent for replica in replica_list if replica.memory_percent is not None]

    app_values: dict[str, list[float]] = {}
    for replica in replica_list:
        for key, value in replica.app_metrics.items():
            app_values.setdefault(key, []).append(value)

    aggregation_mode = {rule.name: rule.aggregation.lower() for rule in rules}
    aggregated_apps: dict[str, float] = {}
    for key, values in app_values.items():
        mode = aggregation_mode.get(key, "max")
        aggregated_apps[key] = sum(values) / len(values) if mode == "avg" else max(values)

    return AggregatedMetrics(
        cpu_percent=(sum(cpu_values) / len(cpu_values)) if cpu_values else None,
        memory_percent=(sum(memory_values) / len(memory_values)) if memory_values else None,
        app_metrics=aggregated_apps,
        sample_size=len(replica_list),
    )
