package com.example.bizservice.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bizservice.model.DependenciesResponse;
import com.example.bizservice.model.DependencyStatus;
import com.example.bizservice.service.DependencyStatusService;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for the dependency verification endpoint.
 */
@WebMvcTest(DependenciesController.class)
class DependenciesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DependencyStatusService dependencyStatusService;

    /**
     * Verifies the dependency endpoint returns PostgreSQL and Redis availability.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldReturnDependencyStatusPayload() throws Exception {
        DependenciesResponse dependencies = new DependenciesResponse(
                "UP",
                new DependencyStatus(
                        "postgres",
                        true,
                        "PostgreSQL connection is available.",
                        Map.of("database", "bizdb", "schema", "public")),
                new DependencyStatus(
                        "redis",
                        true,
                        "Redis connection is available.",
                        Map.of("host", "redis", "ping", "PONG")),
                Instant.parse("2026-03-11T04:50:05Z"));

        given(dependencyStatusService.inspectDependencies()).willReturn(dependencies);

        mockMvc.perform(get("/api/dependencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallStatus").value("UP"))
                .andExpect(jsonPath("$.postgres.name").value("postgres"))
                .andExpect(jsonPath("$.postgres.available").value(true))
                .andExpect(jsonPath("$.postgres.details.database").value("bizdb"))
                .andExpect(jsonPath("$.redis.name").value("redis"))
                .andExpect(jsonPath("$.redis.available").value(true))
                .andExpect(jsonPath("$.redis.details.ping").value("PONG"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
