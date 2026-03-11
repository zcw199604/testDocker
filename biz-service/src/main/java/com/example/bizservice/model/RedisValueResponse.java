package com.example.bizservice.model;

import java.time.Instant;

/**
 * Response body for a Redis key/value lookup or write.
 *
 * @param key redis key.
 * @param value redis value.
 * @param exists whether the key currently exists.
 * @param ttlSeconds remaining TTL in seconds, or null when not applicable.
 * @param timestamp response generation timestamp.
 */
public record RedisValueResponse(
        String key,
        String value,
        boolean exists,
        Long ttlSeconds,
        Instant timestamp) {
}
