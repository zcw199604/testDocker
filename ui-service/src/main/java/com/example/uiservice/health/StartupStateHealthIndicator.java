package com.example.uiservice.health;

import com.example.uiservice.state.StartupState;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Publishes startup completion data to Spring Boot Actuator health.
 */
@Component
public class StartupStateHealthIndicator implements HealthIndicator {

    private final StartupState startupState;

    /**
     * Creates an actuator health indicator backed by startup state.
     *
     * @param startupState startup lifecycle tracker.
     */
    public StartupStateHealthIndicator(StartupState startupState) {
        this.startupState = startupState;
    }

    /**
     * Returns actuator health for the current startup state.
     *
     * @return health object with startup details.
     */
    @Override
    public Health health() {
        Health.Builder builder = startupState.isStartupCompleted()
                ? Health.up()
                : Health.outOfService();
        return builder
                .withDetail("startupCompleted", startupState.isStartupCompleted())
                .withDetail("startedAt", startupState.getStartedAt())
                .withDetail("readyAt", startupState.getReadyAt())
                .build();
    }
}
