package com.example.bizservice.controller;

import com.example.bizservice.model.DependenciesResponse;
import com.example.bizservice.model.HealthResponse;
import com.example.bizservice.service.DependencyStatusService;
import com.example.bizservice.state.StartupState;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes a custom health endpoint for service monitoring.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final StartupState startupState;
    private final DependencyStatusService dependencyStatusService;
    private final String applicationName;

    /**
     * Creates a controller for returning runtime health data.
     *
     * @param startupState startup lifecycle tracker.
     * @param dependencyStatusService dependency validation service.
     * @param applicationName configured Spring application name.
     */
    public HealthController(
            StartupState startupState,
            DependencyStatusService dependencyStatusService,
            @Value("${spring.application.name:biz-service}") String applicationName) {
        this.startupState = startupState;
        this.dependencyStatusService = dependencyStatusService;
        this.applicationName = applicationName;
    }

    /**
     * Returns current service state and startup completion details.
     *
     * @return custom health response.
     */
    @GetMapping("/health")
    public HealthResponse health() {
        boolean startupCompleted = startupState.isStartupCompleted();
        DependenciesResponse dependencies = dependencyStatusService.inspectDependencies();
        String status = resolveStatus(startupCompleted, dependencies.overallStatus());
        return new HealthResponse(
                applicationName,
                status,
                startupCompleted,
                startupState.getStartedAt(),
                startupState.getReadyAt(),
                startupState.getUptime().toSeconds(),
                dependencies,
                Instant.now());
    }

    private String resolveStatus(boolean startupCompleted, String dependencyStatus) {
        if (startupState.isShuttingDown()) {
            return "STOPPING";
        }
        if (!startupCompleted) {
            return "STARTING";
        }
        return "UP".equalsIgnoreCase(dependencyStatus) ? "UP" : "DEGRADED";
    }
}
