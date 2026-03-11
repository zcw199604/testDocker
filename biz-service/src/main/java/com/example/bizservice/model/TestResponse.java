package com.example.bizservice.model;

import java.time.Instant;

/**
 * Response body for the sample test endpoint.
 *
 * @param endpoint endpoint identifier.
 * @param message response message.
 * @param requestedName input name used by the endpoint.
 * @param timestamp response generation timestamp.
 */
public record TestResponse(
        String endpoint,
        String message,
        String requestedName,
        Instant timestamp) {
}
