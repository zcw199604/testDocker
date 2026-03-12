"""Entrypoint for the autoscale agent."""

from __future__ import annotations

import logging

from autoscale_agent.compose_loader import load_service_template
from autoscale_agent.config import AgentSettings
from autoscale_agent.controller import AutoscaleController


def main() -> None:
    """Loads configuration and starts the autoscale loop."""

    logging.basicConfig(
        level=logging.INFO,
        format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
    )
    settings = AgentSettings.from_env()
    template = load_service_template(settings.compose_file, settings.target_service)
    controller = AutoscaleController(settings, template)
    controller.run_forever()
