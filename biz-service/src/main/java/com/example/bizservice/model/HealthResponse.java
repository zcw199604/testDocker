package com.example.bizservice.model;

import java.time.Instant;

/**
 * Response body for the custom health endpoint.
 *
 * @param serviceName current application name.
 * @param status current overall health status.
 * @param startupCompleted whether startup has completed.
 * @param startedAt application start timestamp.
 * @param readyAt application ready timestamp.
 * @param uptimeSeconds current uptime in seconds.
 * @param dependencies dependency availability details.
 * @param timestamp response generation timestamp.
 */
public record HealthResponse(
        String serviceName,
        String status,
        boolean startupCompleted,
        Instant startedAt,
        Instant readyAt,
        long uptimeSeconds,
        DependenciesResponse dependencies,
        Instant timestamp) {
}
