package com.example.bizservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bizservice.model.RedisValueResponse;
import com.example.bizservice.service.RedisValueService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for Redis key/value endpoints.
 */
@WebMvcTest(RedisValueController.class)
class RedisValueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RedisValueService redisValueService;

    /**
     * Verifies a Redis value can be stored.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldStoreRedisValue() throws Exception {
        RedisValueResponse response = new RedisValueResponse(
                "demo:key",
                "hello",
                true,
                120L,
                Instant.parse("2026-03-11T05:46:00Z"));
        given(redisValueService.putValue(any())).willReturn(response);

        mockMvc.perform(post("/api/redis/kv")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"key":"demo:key","value":"hello","ttlSeconds":120}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("demo:key"))
                .andExpect(jsonPath("$.value").value("hello"))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.ttlSeconds").value(120));
    }

    /**
     * Verifies a Redis value can be read.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldReadRedisValue() throws Exception {
        RedisValueResponse response = new RedisValueResponse(
                "demo:key",
                "hello",
                true,
                120L,
                Instant.parse("2026-03-11T05:46:01Z"));
        given(redisValueService.getValue("demo:key")).willReturn(response);

        mockMvc.perform(get("/api/redis/kv/demo:key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("demo:key"))
                .andExpect(jsonPath("$.value").value("hello"))
                .andExpect(jsonPath("$.exists").value(true));
    }
}
