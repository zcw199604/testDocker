package com.example.uiservice.service;

import com.example.uiservice.model.PutUiSessionRequest;
import com.example.uiservice.model.UiSessionResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles real Redis read/write operations for UI session data.
 */
@Service
public class UiSessionService {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Creates a UI session service.
     *
     * @param stringRedisTemplate redis template.
     * @param objectMapper object mapper.
     */
    public UiSessionService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Stores a UI session in Redis.
     *
     * @param request put request.
     * @return stored session response.
     */
    public UiSessionResponse putSession(PutUiSessionRequest request) {
        validateRequest(request);
        String key = buildKey(request.sessionId());
        String value = serializeValue(request.userId().trim(), request.page().trim());
        Long ttlSeconds = normalizeTtl(request.ttlSeconds());
        if (ttlSeconds != null) {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
        return getSession(request.sessionId());
    }

    /**
     * Reads a UI session from Redis.
     *
     * @param sessionId session id.
     * @return session response.
     */
    public UiSessionResponse getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must not be blank.");
        }
        String key = buildKey(sessionId);
        String rawValue = stringRedisTemplate.opsForValue().get(key);
        Long expire = stringRedisTemplate.getExpire(key);
        Map<String, String> payload = deserializeValue(rawValue);
        return new UiSessionResponse(
                key,
                payload.get("userId"),
                payload.get("page"),
                rawValue,
                rawValue != null,
                normalizeExpire(expire),
                Instant.now());
    }

    private void validateRequest(PutUiSessionRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.sessionId() == null || request.sessionId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sessionId must not be blank.");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank.");
        }
        if (request.page() == null || request.page().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must not be blank.");
        }
        if (request.ttlSeconds() != null && request.ttlSeconds() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ttlSeconds must be greater than 0.");
        }
    }

    private String buildKey(String sessionId) {
        return "ui:session:" + sessionId.trim();
    }

    private Long normalizeTtl(Long ttlSeconds) {
        return ttlSeconds == null ? null : ttlSeconds;
    }

    private Long normalizeExpire(Long expire) {
        if (expire == null || expire < 0) {
            return null;
        }
        return expire;
    }

    private String serializeValue(String userId, String page) {
        try {
            return objectMapper.writeValueAsString(Map.of("userId", userId, "page", page));
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize UI session payload.");
        }
    }

    private Map<String, String> deserializeValue(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(rawValue, new TypeReference<Map<String, String>>() { });
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize UI session payload.");
        }
    }
}
