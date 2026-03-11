package com.example.bizservice.model;

/**
 * Request body for storing a Redis key/value pair.
 *
 * @param key redis key.
 * @param value redis value.
 * @param ttlSeconds optional TTL in seconds.
 */
public record PutRedisValueRequest(String key, String value, Long ttlSeconds) {
}
