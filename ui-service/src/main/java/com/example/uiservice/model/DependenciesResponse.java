package com.example.uiservice.model;

import java.time.Instant;

/**
 * Response body for validating PostgreSQL and Redis availability.
 *
 * @param overallStatus aggregate dependency status.
 * @param postgres PostgreSQL validation result.
 * @param redis Redis validation result.
 * @param timestamp response generation timestamp.
 */
public record DependenciesResponse(
        String overallStatus,
        DependencyStatus postgres,
        DependencyStatus redis,
        Instant timestamp) {
}
