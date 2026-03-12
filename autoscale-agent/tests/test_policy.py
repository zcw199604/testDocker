"""Tests for autoscale scaling policy."""

from __future__ import annotations

from datetime import datetime, timedelta, timezone
from pathlib import Path
import sys
import unittest

CURRENT_DIR = Path(__file__).resolve().parent
PACKAGE_ROOT = CURRENT_DIR.parent
sys.path.insert(0, str(PACKAGE_ROOT))

from autoscale_agent.config import AgentSettings, AppMetricRule
from autoscale_agent.metrics import AggregatedMetrics
from autoscale_agent.policy import ScalingPolicy


class ScalingPolicyTest(unittest.TestCase):
    """Verifies scale out and scale in decisions."""

    def setUp(self) -> None:
        self.settings = AgentSettings(
            compose_file=Path('/tmp/docker-compose.yml'),
            generated_dir=Path('/tmp/generated'),
            target_service='biz-service',
            nginx_service='nginx',
            poll_interval_seconds=15,
            min_replicas=1,
            max_replicas=3,
            scale_up_window=2,
            scale_down_window=2,
            cooldown_seconds=60,
            drain_seconds=20,
            startup_timeout_seconds=180,
            http_timeout_seconds=2.5,
            cpu_up_threshold=70.0,
            cpu_down_threshold=25.0,
            memory_up_threshold=80.0,
            memory_down_threshold=35.0,
            app_metric_rules=(
                AppMetricRule(name='requestRatePerSecond', scale_up_threshold=4.0, scale_down_threshold=0.5, aggregation='avg'),
            ),
        )

    def test_should_scale_out_after_required_windows(self) -> None:
        policy = ScalingPolicy(self.settings)
        now = datetime(2026, 3, 11, 14, 0, tzinfo=timezone.utc)
        metrics = AggregatedMetrics(
            cpu_percent=75.0,
            memory_percent=40.0,
            app_metrics={'requestRatePerSecond': 5.0},
            sample_size=1,
        )

        self.assertEqual(policy.evaluate(metrics, total_replicas=1, managed_replicas=0, now=now).action, 'hold')
        self.assertEqual(
            policy.evaluate(metrics, total_replicas=1, managed_replicas=0, now=now + timedelta(seconds=15)).action,
            'scale_up',
        )

    def test_should_scale_in_after_required_windows(self) -> None:
        policy = ScalingPolicy(self.settings)
        now = datetime(2026, 3, 11, 14, 0, tzinfo=timezone.utc)
        metrics = AggregatedMetrics(
            cpu_percent=20.0,
            memory_percent=25.0,
            app_metrics={'requestRatePerSecond': 0.1},
            sample_size=2,
        )

        self.assertEqual(policy.evaluate(metrics, total_replicas=2, managed_replicas=1, now=now).action, 'hold')
        self.assertEqual(
            policy.evaluate(metrics, total_replicas=2, managed_replicas=1, now=now + timedelta(seconds=15)).action,
            'scale_down',
        )

    def test_should_observe_cooldown(self) -> None:
        policy = ScalingPolicy(self.settings)
        now = datetime(2026, 3, 11, 14, 0, tzinfo=timezone.utc)
        metrics = AggregatedMetrics(
            cpu_percent=75.0,
            memory_percent=40.0,
            app_metrics={'requestRatePerSecond': 5.0},
            sample_size=1,
        )

        policy.evaluate(metrics, total_replicas=1, managed_replicas=0, now=now)
        self.assertEqual(
            policy.evaluate(metrics, total_replicas=1, managed_replicas=0, now=now + timedelta(seconds=15)).action,
            'scale_up',
        )
        self.assertEqual(
            policy.evaluate(metrics, total_replicas=2, managed_replicas=1, now=now + timedelta(seconds=30)).action,
            'hold',
        )


if __name__ == '__main__':
    unittest.main()
