package com.example.uiservice.model;

import java.time.Instant;
import java.util.List;

/**
 * Response body for the ui-service root entry.
 *
 * @param serviceName current application name.
 * @param role current service role.
 * @param availableRoutes routes exposed by the service.
 * @param timestamp response generation timestamp.
 */
public record RootResponse(
        String serviceName,
        String role,
        List<String> availableRoutes,
        Instant timestamp) {

    /**
     * Normalizes routes to an immutable list.
     */
    public RootResponse {
        availableRoutes = availableRoutes == null ? List.of() : List.copyOf(availableRoutes);
    }
}
