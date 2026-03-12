"""Tests for autoscale agent environment configuration."""

from __future__ import annotations

from pathlib import Path
import sys
import unittest

CURRENT_DIR = Path(__file__).resolve().parent
PACKAGE_ROOT = CURRENT_DIR.parent
sys.path.insert(0, str(PACKAGE_ROOT))

from autoscale_agent.config import AgentSettings


class AgentSettingsTest(unittest.TestCase):
    """Verifies environment configuration parsing."""

    def test_should_parse_boolean_guards(self) -> None:
        settings = AgentSettings.from_env(
            {
                'AUTOSCALE_COMPOSE_FILE': '/tmp/docker-compose.yml',
                'AUTOSCALE_GENERATED_DIR': '/tmp/generated',
                'AUTOSCALE_DRY_RUN': 'true',
                'AUTOSCALE_FAILURE_FREEZE': 'false',
                'AUTOSCALE_APP_METRIC_RULES': '[]',
            }
        )

        self.assertEqual(settings.compose_file, Path('/tmp/docker-compose.yml'))
        self.assertEqual(settings.generated_dir, Path('/tmp/generated'))
        self.assertTrue(settings.dry_run)
        self.assertFalse(settings.failure_freeze)


if __name__ == '__main__':
    unittest.main()
