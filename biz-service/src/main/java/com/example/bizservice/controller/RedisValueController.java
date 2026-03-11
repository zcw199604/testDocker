package com.example.bizservice.controller;

import com.example.bizservice.model.PutRedisValueRequest;
import com.example.bizservice.model.RedisValueResponse;
import com.example.bizservice.service.RedisValueService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes real Redis key/value read/write endpoints.
 */
@RestController
@RequestMapping("/api/redis/kv")
public class RedisValueController {

    private final RedisValueService redisValueService;

    /**
     * Creates a Redis key/value controller.
     *
     * @param redisValueService Redis value service.
     */
    public RedisValueController(RedisValueService redisValueService) {
        this.redisValueService = redisValueService;
    }

    /**
     * Stores a key/value pair in Redis.
     *
     * @param request put request.
     * @return stored value response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RedisValueResponse put(@RequestBody PutRedisValueRequest request) {
        return redisValueService.putValue(request);
    }

    /**
     * Reads a key/value pair from Redis.
     *
     * @param key redis key.
     * @return value response.
     */
    @GetMapping("/{key}")
    public RedisValueResponse get(@PathVariable String key) {
        return redisValueService.getValue(key);
    }
}
