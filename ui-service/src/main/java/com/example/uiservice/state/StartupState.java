package com.example.uiservice.state;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Tracks the startup lifecycle of the current service instance.
 */
@Component
public class StartupState {

    private final Instant startedAt = Instant.now();
    private final AtomicBoolean startupCompleted = new AtomicBoolean(false);
    private volatile Instant readyAt;

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
     * Returns current uptime from application start.
     *
     * @return uptime duration.
     */
    public Duration getUptime() {
        return Duration.between(startedAt, Instant.now());
    }
}
