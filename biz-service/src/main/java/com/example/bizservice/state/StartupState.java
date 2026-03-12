package com.example.bizservice.state;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tracks the startup lifecycle of the current service instance.
 */
@Component
public class StartupState {

    private final Instant startedAt = Instant.now();
    private final AtomicBoolean startupCompleted = new AtomicBoolean(false);
    private final AtomicBoolean shuttingDown = new AtomicBoolean(false);
    private volatile Instant readyAt;
    private volatile Instant shutdownStartedAt;

    /**
     * Marks the service as ready after Spring Boot startup completes.
     *
     * @param event application ready event.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void handleApplicationReady(ApplicationReadyEvent event) {
        this.readyAt = Instant.now();
        this.startupCompleted.set(true);
    }

    /**
     * Marks the service as shutting down when the Spring context closes.
     *
     * @param event application context closed event.
     */
    @EventListener(ContextClosedEvent.class)
    public void handleContextClosed(ContextClosedEvent event) {
        this.shutdownStartedAt = Instant.now();
        this.shuttingDown.set(true);
    }

    /**
     * Returns whether the service completed startup.
     *
     * @return true when the service is ready.
     */
    public boolean isStartupCompleted() {
        return startupCompleted.get();
    }

    /**
     * Returns the timestamp when startup tracking began.
     *
     * @return application start timestamp.
     */
    public Instant getStartedAt() {
        return startedAt;
    }

    /**
     * Returns the timestamp when the application became ready.
     *
     * @return ready timestamp, or null when startup is still in progress.
     */
    public Instant getReadyAt() {
        return readyAt;
    }

    /**
     * Returns whether the service is in its shutdown phase.
     *
     * @return true when shutdown has started.
     */
    public boolean isShuttingDown() {
        return shuttingDown.get();
    }

    /**
     * Returns whether the service is ready to receive traffic.
     *
     * @return true when startup has completed and shutdown has not started.
     */
    public boolean isReady() {
        return isStartupCompleted() && !isShuttingDown();
    }

    /**
     * Returns the timestamp when shutdown started.
     *
     * @return shutdown timestamp, or null when shutdown has not started.
     */
    public Instant getShutdownStartedAt() {
        return shutdownStartedAt;
    }

    /**
     * Returns the current lifecycle status of this instance.
     *
     * @return STARTING, READY, or STOPPING depending on lifecycle state.
     */
    public String currentStatus() {
        if (isShuttingDown()) {
            return "STOPPING";
        }
        return isReady() ? "READY" : "STARTING";
    }

    /**
     * Returns current uptime from application start.
     *
     * @return uptime duration.
     */
    public Duration getUptime() {
        return Duration.between(startedAt, Instant.now());
    }
}
