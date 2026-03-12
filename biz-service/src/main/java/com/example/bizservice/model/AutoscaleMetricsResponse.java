package com.example.bizservice.model;

import java.time.Instant;

/**
 * Response body for the autoscale metrics endpoint.
 *
 * @param serviceName service identifier.
 * @param instanceId runtime instance identifier.
 * @param status current service state.
 * @param startupCompleted whether startup has completed.
 * @param inflightRequests current inflight business requests.
 * @param windowRequestCount request count within the rolling window.
 * @param windowErrorCount error count within the rolling window.
 * @param requestRatePerSecond estimated requests per second within the rolling window.
 * @param averageResponseTimeMs average response time within the rolling window.
 * @param windowStartedAt rolling window start time.
 * @param timestamp response timestamp.
 */
public record AutoscaleMetricsResponse(
        String serviceName,
        String instanceId,
        String status,
        boolean startupCompleted,
        int inflightRequests,
        long windowRequestCount,
        long windowErrorCount,
        double requestRatePerSecond,
        double averageResponseTimeMs,
        Instant windowStartedAt,
        Instant timestamp) {
}
