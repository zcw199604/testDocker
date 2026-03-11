package com.example.uiservice.model;

/**
 * Request body for storing a UI session in Redis.
 *
 * @param sessionId session identifier.
 * @param userId frontend user identifier.
 * @param page current page path or name.
 * @param ttlSeconds optional TTL in seconds.
 */
public record PutUiSessionRequest(String sessionId, String userId, String page, Long ttlSeconds) {
}
