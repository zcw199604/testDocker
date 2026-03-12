"""Main autoscaling loop and orchestration."""

from __future__ import annotations

from datetime import datetime, timezone
import logging
import time

from autoscale_agent.compose_loader import ServiceTemplate
from autoscale_agent.config import AgentSettings
from autoscale_agent.metrics import aggregate_metrics
from autoscale_agent.nginx_control import Backend, NginxController
from autoscale_agent.policy import ScalingPolicy
from autoscale_agent.runtime import DockerRuntime, Replica

LOGGER = logging.getLogger(__name__)


class AutoscaleController:
    """Coordinates runtime discovery, policy evaluation, and nginx updates."""

    def __init__(self, settings: AgentSettings, template: ServiceTemplate) -> None:
        self._settings = settings
        self._template = template
        self._runtime = DockerRuntime(settings, template)
        self._policy = ScalingPolicy(settings)
        self._nginx = NginxController(settings, template, self._runtime.client)
        self._frozen_reason: str | None = None

    def run_forever(self) -> None:
        """Starts the long-running autoscale loop."""

        LOGGER.info("Autoscale agent started for %s/%s", self._template.project_name, self._template.service_name)
        while True:
            try:
                self.step()
            except Exception as exc:  # pragma: no cover - defensive main loop
                LOGGER.exception("Autoscale loop iteration failed")
                if self._settings.failure_freeze:
                    self._frozen_reason = str(exc) or exc.__class__.__name__
                    LOGGER.error("Autoscale loop is frozen because failure_freeze is enabled: %s", self._frozen_reason)
            time.sleep(self._settings.poll_interval_seconds)

    def step(self) -> None:
        """Executes one autoscale evaluation cycle."""

        if self._frozen_reason is not None:
            LOGGER.warning("Autoscale actions are frozen: %s", self._frozen_reason)
            return

        replicas = self._runtime.list_replicas()
        if not replicas:
            LOGGER.warning("No replicas found for service %s", self._template.service_name)
            return

        observed = [self._runtime.populate_runtime_metrics(replica) for replica in replicas if replica.is_running]
        self._sync_nginx(observed)

        total_replicas = len(observed)
        managed_replicas = len([replica for replica in observed if replica.role == "managed"])
        metrics = aggregate_metrics([replica for replica in observed if replica.is_healthy], self._settings.app_metric_rules)
        decision = self._policy.evaluate(
            metrics=metrics,
            total_replicas=total_replicas,
            managed_replicas=managed_replicas,
            now=datetime.now(timezone.utc),
        )
        LOGGER.info("Policy decision=%s reason=%s", decision.action, decision.reason)

        if self._settings.dry_run and decision.action in {"scale_up", "scale_down"}:
            LOGGER.info("Dry-run enabled, skipped %s action", decision.action)
            return

        if decision.action == "scale_up":
            self._scale_up(observed)
        elif decision.action == "scale_down":
            self._scale_down(observed)

    def _scale_up(self, replicas: list[Replica]) -> None:
        seed = next((replica for replica in replicas if replica.role == "seed" and replica.is_running), None)
        if seed is None:
            LOGGER.warning("Scale-up skipped because no running seed replica is available")
            return

        managed = self._runtime.create_managed_replica(seed)
        if not self._runtime.wait_until_healthy(managed, self._settings.startup_timeout_seconds):
            self._runtime.remove_replica_immediately(managed)
            raise RuntimeError(f"Managed replica {managed.name} failed health checks in time.")

        replicas = self._runtime.list_replicas()
        observed = [self._runtime.populate_runtime_metrics(replica) for replica in replicas if replica.is_running]
        self._sync_nginx(observed)
        LOGGER.info("Scale-up completed with new managed replica %s", managed.name)

    def _scale_down(self, replicas: list[Replica]) -> None:
        managed_candidates = [replica for replica in replicas if replica.role == "managed" and replica.is_running]
        if not managed_candidates:
            LOGGER.info("Scale-down skipped because no managed replica is available")
            return

        target = sorted(managed_candidates, key=lambda item: item.created_at, reverse=True)[0]
        remaining = [replica for replica in replicas if replica.name != target.name]
        self._sync_nginx(remaining)
        LOGGER.info(
            "Removed %s from upstream, waiting up to %ss for in-flight requests to drain",
            target.name,
            self._settings.drain_seconds,
        )
        self._runtime.wait_for_inflight_drain(target, self._settings.drain_seconds)
        self._runtime.stop_and_remove(target)

    def _sync_nginx(self, replicas: list[Replica]) -> None:
        backends = self._build_backends(replicas)
        self._nginx.apply_backends(backends)

    def _build_backends(self, replicas: list[Replica]) -> list[Backend]:
        backends: list[Backend] = []
        for replica in sorted(replicas, key=lambda item: item.name):
            if not replica.is_healthy:
                continue
            if replica.role == "seed":
                backends.append(
                    Backend(
                        target=f"{self._template.seed_host}:{self._template.target_port}",
                        comment=f"seed {replica.name}",
                    )
                )
                continue
            if replica.role == "managed" and replica.addresses:
                backends.append(
                    Backend(
                        target=f"{replica.addresses[0]}:{self._template.target_port}",
                        comment=f"managed {replica.name}",
                    )
                )
        return backends
