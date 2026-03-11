package com.example.uiservice.model;

import java.util.Map;

/**
 * Describes availability information for a single dependency.
 *
 * @param name dependency name.
 * @param available whether the dependency is reachable.
 * @param message human-readable validation result.
 * @param details structured diagnostic details.
 */
public record DependencyStatus(
        String name,
        boolean available,
        String message,
        Map<String, String> details) {

    /**
     * Normalizes optional details to an immutable map.
     */
    public DependencyStatus {
        details = details == null ? Map.of() : Map.copyOf(details);
    }
}
