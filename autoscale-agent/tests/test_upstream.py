"""Unit tests for nginx upstream rendering."""

from __future__ import annotations

from pathlib import Path
import sys
import unittest

CURRENT_DIR = Path(__file__).resolve().parent
PACKAGE_ROOT = CURRENT_DIR.parent
sys.path.insert(0, str(PACKAGE_ROOT))

from autoscale_agent.nginx_control import Backend, render_upstream


class RenderUpstreamTest(unittest.TestCase):
    """Ensures upstream output contains expected backends."""

    def test_renders_seed_and_managed_backends(self) -> None:
        content = render_upstream(
            upstream_name='biz_service_upstream',
            backends=[
                Backend(target='biz-service:8080', comment='seed testdocker-biz-service-1'),
                Backend(target='172.22.0.15:8080', comment='managed testdocker-biz-service-managed-1'),
            ],
        )

        self.assertIn('upstream biz_service_upstream', content)
        self.assertIn('server biz-service:8080 max_fails=3 fail_timeout=10s;', content)
        self.assertIn('server 172.22.0.15:8080 max_fails=3 fail_timeout=10s;', content)
        self.assertIn('keepalive 32;', content)

    def test_renders_placeholder_when_no_healthy_backends_exist(self) -> None:
        content = render_upstream(upstream_name='biz_service_upstream', backends=[])

        self.assertIn('server 127.0.0.1:65535 down;', content)
        self.assertIn('# no-healthy-backends', content)


if __name__ == '__main__':
    unittest.main()
