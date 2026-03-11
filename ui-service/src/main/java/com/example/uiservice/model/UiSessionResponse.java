package com.example.uiservice.model;

import java.time.Instant;

/**
 * Response body for a UI session lookup or write.
 *
 * @param key redis key.
 * @param userId frontend user identifier.
 * @param page current page path or name.
 * @param rawValue serialized value stored in Redis.
 * @param exists whether the key currently exists.
 * @param ttlSeconds remaining TTL in seconds, or null when not applicable.
 * @param timestamp response generation timestamp.
 */
public record UiSessionResponse(
        String key,
        String userId,
        String page,
        String rawValue,
        boolean exists,
        Long ttlSeconds,
        Instant timestamp) {
}
