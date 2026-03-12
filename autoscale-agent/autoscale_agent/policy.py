"""Window-based autoscaling policy."""

from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta, timezone

from autoscale_agent.config import AgentSettings
from autoscale_agent.metrics import AggregatedMetrics


@dataclass(frozen=True, slots=True)
class ScaleDecision:
    """Result of one policy evaluation."""

    action: str
    reason: str


class ScalingPolicy:
    """Applies threshold windows and cooldowns to scaling decisions."""

    def __init__(self, settings: AgentSettings) -> None:
        self._settings = settings
        self._up_hits = 0
        self._down_hits = 0
        self._last_scaled_at: datetime | None = None

    def evaluate(
        self,
        metrics: AggregatedMetrics,
        total_replicas: int,
        managed_replicas: int,
        now: datetime | None = None,
    ) -> ScaleDecision:
        """Evaluates whether to scale up, scale down, or hold."""

        current_time = now or datetime.now(timezone.utc)

        if total_replicas < self._settings.min_replicas:
            self._reset_windows()
            self._last_scaled_at = current_time
            return ScaleDecision("scale_up", "当前副本数低于最小副本数。")

        if total_replicas > self._settings.max_replicas and managed_replicas > 0:
            self._reset_windows()
            self._last_scaled_at = current_time
            return ScaleDecision("scale_down", "当前副本数高于最大副本数。")

        if self._is_in_cooldown(current_time):
            return ScaleDecision("hold", "冷却时间内，保持当前副本数。")

        upscale_reasons = self._upscale_reasons(metrics)
        if upscale_reasons:
            self._up_hits += 1
            self._down_hits = 0
            if total_replicas >= self._settings.max_replicas:
                return ScaleDecision("hold", "已达到最大副本数。")
            if self._up_hits >= self._settings.scale_up_window:
                self._mark_scaled(current_time)
                return ScaleDecision("scale_up", "；".join(upscale_reasons))
            return ScaleDecision(
                "hold",
                f"扩容条件已连续命中 {self._up_hits}/{self._settings.scale_up_window} 次。",
            )

        downscale_reasons = self._downscale_reasons(metrics)
        if downscale_reasons:
            self._down_hits += 1
            self._up_hits = 0
            if managed_replicas <= 0 or total_replicas <= self._settings.min_replicas:
                return ScaleDecision("hold", "没有可缩容的 managed 副本。")
            if self._down_hits >= self._settings.scale_down_window:
                self._mark_scaled(current_time)
                return ScaleDecision("scale_down", "；".join(downscale_reasons))
            return ScaleDecision(
                "hold",
                f"缩容条件已连续命中 {self._down_hits}/{self._settings.scale_down_window} 次。",
            )

        self._reset_windows()
        return ScaleDecision("hold", "当前指标未触发扩缩容阈值。")

    def _upscale_reasons(self, metrics: AggregatedMetrics) -> list[str]:
        reasons: list[str] = []
        if metrics.cpu_percent is not None and metrics.cpu_percent >= self._settings.cpu_up_threshold:
            reasons.append(
                f"平均 CPU {metrics.cpu_percent:.1f}% ≥ {self._settings.cpu_up_threshold:.1f}%"
            )
        if metrics.memory_percent is not None and metrics.memory_percent >= self._settings.memory_up_threshold:
            reasons.append(
                f"平均内存 {metrics.memory_percent:.1f}% ≥ {self._settings.memory_up_threshold:.1f}%"
            )
        for rule in self._settings.app_metric_rules:
            value = metrics.app_metrics.get(rule.name)
            if value is not None and value >= rule.scale_up_threshold:
                reasons.append(f"应用指标 {rule.name}={value:.2f} ≥ {rule.scale_up_threshold:.2f}")
        return reasons

    def _downscale_reasons(self, metrics: AggregatedMetrics) -> list[str]:
        reasons: list[str] = []
        if metrics.cpu_percent is None or metrics.memory_percent is None:
            return reasons
        if metrics.cpu_percent > self._settings.cpu_down_threshold:
            return []
        if metrics.memory_percent > self._settings.memory_down_threshold:
            return []
        for rule in self._settings.app_metric_rules:
            value = metrics.app_metrics.get(rule.name)
            if value is None or value > rule.scale_down_threshold:
                return []
        reasons.append(
            f"平均 CPU {metrics.cpu_percent:.1f}% ≤ {self._settings.cpu_down_threshold:.1f}%"
        )
        reasons.append(
            f"平均内存 {metrics.memory_percent:.1f}% ≤ {self._settings.memory_down_threshold:.1f}%"
        )
        for rule in self._settings.app_metric_rules:
            value = metrics.app_metrics[rule.name]
            reasons.append(f"应用指标 {rule.name}={value:.2f} ≤ {rule.scale_down_threshold:.2f}")
        return reasons

    def _is_in_cooldown(self, now: datetime) -> bool:
        if self._last_scaled_at is None:
            return False
        return now - self._last_scaled_at < timedelta(seconds=self._settings.cooldown_seconds)

    def _mark_scaled(self, now: datetime) -> None:
        self._last_scaled_at = now
        self._reset_windows()

    def _reset_windows(self) -> None:
        self._up_hits = 0
        self._down_hits = 0
