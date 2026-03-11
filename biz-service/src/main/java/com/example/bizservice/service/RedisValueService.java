package com.example.bizservice.service;

import com.example.bizservice.model.PutRedisValueRequest;
import com.example.bizservice.model.RedisValueResponse;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles real Redis read/write operations for key/value data.
 */
@Service
public class RedisValueService {

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Creates a Redis value service.
     *
     * @param stringRedisTemplate redis template.
     */
    public RedisValueService(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * Stores a key/value pair in Redis.
     *
     * @param request put request.
     * @return stored value response.
     */
    public RedisValueResponse putValue(PutRedisValueRequest request) {
        validatePutRequest(request);
        String key = request.key().trim();
        String value = request.value();
        Long ttlSeconds = normalizeTtl(request.ttlSeconds());
        if (ttlSeconds != null) {
            stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(ttlSeconds));
        } else {
            stringRedisTemplate.opsForValue().set(key, value);
        }
        return getValue(key);
    }

    /**
     * Reads a key/value pair from Redis.
     *
     * @param key redis key.
     * @return value response.
     */
    public RedisValueResponse getValue(String key) {
        if (key == null || key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key must not be blank.");
        }
        String normalizedKey = key.trim();
        String value = stringRedisTemplate.opsForValue().get(normalizedKey);
        Long expire = stringRedisTemplate.getExpire(normalizedKey);
        return new RedisValueResponse(
                normalizedKey,
                value,
                value != null,
                normalizeExpire(expire),
                Instant.now());
    }

    private void validatePutRequest(PutRedisValueRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.key() == null || request.key().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "key must not be blank.");
        }
        if (request.value() == null || request.value().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "value must not be blank.");
        }
        if (request.ttlSeconds() != null && request.ttlSeconds() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ttlSeconds must be greater than 0.");
        }
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
}
