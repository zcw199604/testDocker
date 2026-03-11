package com.example.bizservice.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bizservice.model.DependenciesResponse;
import com.example.bizservice.model.DependencyStatus;
import com.example.bizservice.service.DependencyStatusService;
import com.example.bizservice.state.StartupState;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for the custom health endpoint.
 */
@WebMvcTest(HealthController.class)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StartupState startupState;

    @MockBean
    private DependencyStatusService dependencyStatusService;

    /**
     * Verifies the health endpoint returns startup and dependency details.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldReturnAggregatedHealthPayload() throws Exception {
        Instant startedAt = Instant.parse("2026-03-11T04:50:00Z");
        Instant readyAt = Instant.parse("2026-03-11T04:50:03Z");
        DependenciesResponse dependencies = new DependenciesResponse(
                "UP",
                new DependencyStatus(
                        "postgres",
                        true,
                        "PostgreSQL connection is available.",
                        Map.of("database", "bizdb")),
                new DependencyStatus(
                        "redis",
                        true,
                        "Redis connection is available.",
                        Map.of("ping", "PONG")),
                Instant.parse("2026-03-11T04:50:05Z"));

        given(startupState.isStartupCompleted()).willReturn(true);
        given(startupState.getStartedAt()).willReturn(startedAt);
        given(startupState.getReadyAt()).willReturn(readyAt);
        given(startupState.getUptime()).willReturn(Duration.ofSeconds(12));
        given(dependencyStatusService.inspectDependencies()).willReturn(dependencies);

        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("biz-service"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.startupCompleted").value(true))
                .andExpect(jsonPath("$.dependencies.overallStatus").value("UP"))
                .andExpect(jsonPath("$.dependencies.postgres.available").value(true))
                .andExpect(jsonPath("$.dependencies.redis.available").value(true))
                .andExpect(jsonPath("$.startedAt").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
