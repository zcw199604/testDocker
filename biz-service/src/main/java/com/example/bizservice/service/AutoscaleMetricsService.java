package com.example.bizservice.service;

import com.example.bizservice.config.AutoscaleMetricsProperties;
import com.example.bizservice.model.AutoscaleMetricsResponse;
import com.example.bizservice.state.StartupState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Aggregates autoscale metrics for tracked business API traffic.
 */
@Service
public class AutoscaleMetricsService {

    private static final Set<String> EXCLUDED_PATHS = Set.of(
            "/api/health",
            "/api/dependencies",
            "/api/autoscale/metrics");

    private final StartupState startupState;
    private final Duration windowDuration;
    private final String serviceName;
    private final String instanceId;
    private final Clock clock;
    private final AtomicInteger inflightRequests = new AtomicInteger();
    private final Object monitor = new Object();

    private Instant windowStartedAt;
    private long windowRequestCount;
    private long windowErrorCount;
    private long totalResponseTimeMs;

    /**
     * Creates a production autoscale metrics service.
     *
     * @param startupState startup tracker.
     * @param autoscaleMetricsProperties autoscale properties.
     * @param serviceName configured service name.
     * @param instanceId configured instance identifier.
     */
    @Autowired
    public AutoscaleMetricsService(
            StartupState startupState,
            AutoscaleMetricsProperties autoscaleMetricsProperties,
            @Value("${spring.application.name:biz-service}") String serviceName,
            @Value("${spring.application.instance-id:${HOSTNAME:${random.uuid}}}") String instanceId) {
        this(startupState, autoscaleMetricsProperties, serviceName, instanceId, Clock.systemUTC());
    }

    /**
     * Creates an autoscale metrics service with an explicit clock.
     *
     * @param startupState startup tracker.
     * @param autoscaleMetricsProperties autoscale properties.
     * @param serviceName configured service name.
     * @param instanceId configured instance identifier.
     * @param clock clock used to read timestamps.
     */
    public AutoscaleMetricsService(
            StartupState startupState,
            AutoscaleMetricsProperties autoscaleMetricsProperties,
            String serviceName,
            String instanceId,
            Clock clock) {
        this.startupState = startupState;
        this.windowDuration = autoscaleMetricsProperties.getWindowDuration();
        this.serviceName = serviceName;
        this.instanceId = instanceId;
        this.clock = clock;
        this.windowStartedAt = Instant.now(clock);
    }

    /**
     * Returns whether the request path should be included in autoscale metrics.
     *
     * @param requestPath request path relative to the application context.
     * @return true when the request should be tracked.
     */
    public boolean shouldTrackPath(String requestPath) {
        String normalizedPath = normalizePath(requestPath);
        return normalizedPath.startsWith("/api/") && !EXCLUDED_PATHS.contains(normalizedPath);
    }

    /**
     * Marks the beginning of a tracked business request.
     */
    public void onRequestStarted() {
        inflightRequests.incrementAndGet();
    }

    /**
     * Marks completion of a tracked business request.
     *
     * @param durationMillis request duration in milliseconds.
     * @param responseStatus HTTP response status code.
     */
    public void onRequestFinished(long durationMillis, int responseStatus) {
        onRequestFinished(Duration.ofMillis(Math.max(durationMillis, 0L)), responseStatus, false);
    }

    /**
     * Records completion details for a tracked request.
     *
     * @param responseTime request processing duration.
     * @param responseStatus HTTP response status code.
     * @param failed true when request processing failed with an exception.
     */
    public void onRequestFinished(Duration responseTime, int responseStatus, boolean failed) {
        Instant now = Instant.now(clock);
        long responseTimeMs = Math.max(0L, responseTime.toMillis());

        synchronized (monitor) {
            rotateWindowIfNeeded(now);
            windowRequestCount++;
            if (failed || responseStatus >= 500) {
                windowErrorCount++;
            }
            totalResponseTimeMs += responseTimeMs;
        }

        inflightRequests.updateAndGet(currentValue -> Math.max(0, currentValue - 1));
    }

    /**
     * Creates a rolling autoscale metrics snapshot.
     *
     * @return autoscale metrics payload.
     */
    public AutoscaleMetricsResponse snapshot() {
        Instant now = Instant.now(clock);
        Instant currentWindowStartedAt;
        long currentWindowRequestCount;
        long currentWindowErrorCount;
        long currentTotalResponseTimeMs;

        synchronized (monitor) {
            rotateWindowIfNeeded(now);
            currentWindowStartedAt = windowStartedAt;
            currentWindowRequestCount = windowRequestCount;
            currentWindowErrorCount = windowErrorCount;
            currentTotalResponseTimeMs = totalResponseTimeMs;
        }

        return new AutoscaleMetricsResponse(
                serviceName,
                instanceId,
                startupState.currentStatus(),
                startupState.isStartupCompleted(),
                inflightRequests.get(),
                currentWindowRequestCount,
                currentWindowErrorCount,
                calculateRequestRatePerSecond(currentWindowRequestCount),
                calculateAverageResponseTimeMs(currentWindowRequestCount, currentTotalResponseTimeMs),
                currentWindowStartedAt,
                now);
    }

    private double calculateRequestRatePerSecond(long requestCount) {
        double windowSeconds = Math.max(1d, windowDuration.toMillis() / 1000d);
        return requestCount / windowSeconds;
    }

    private double calculateAverageResponseTimeMs(long requestCount, long currentTotalResponseTimeMs) {
        if (requestCount == 0L) {
            return 0d;
        }
        return currentTotalResponseTimeMs / (double) requestCount;
    }

    private void rotateWindowIfNeeded(Instant now) {
        Duration elapsed = Duration.between(windowStartedAt, now);
        if (!elapsed.isNegative() && elapsed.compareTo(windowDuration) < 0) {
            return;
        }

        windowStartedAt = now;
        windowRequestCount = 0L;
        windowErrorCount = 0L;
        totalResponseTimeMs = 0L;
    }

    private String normalizePath(String requestPath) {
        if (requestPath == null || requestPath.isBlank()) {
            return "";
        }

        String normalizedPath = requestPath.trim();
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            return normalizedPath.substring(0, normalizedPath.length() - 1);
        }
        return normalizedPath;
    }
}
