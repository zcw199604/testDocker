package com.example.uiservice.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.uiservice.model.DependenciesResponse;
import com.example.uiservice.model.DependencyStatus;
import com.example.uiservice.service.DependencyStatusService;
import com.example.uiservice.state.StartupState;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for the ui-service health endpoint.
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
        DependenciesResponse dependencies = new DependenciesResponse(
                "UP",
                new DependencyStatus("postgres", true, "PostgreSQL connection is available.", Map.of("database", "bizdb")),
                new DependencyStatus("redis", true, "Redis connection is available.", Map.of("ping", "PONG")),
                Instant.parse("2026-03-11T09:20:00Z"));

        given(startupState.isStartupCompleted()).willReturn(true);
        given(startupState.getStartedAt()).willReturn(Instant.parse("2026-03-11T09:19:00Z"));
        given(startupState.getReadyAt()).willReturn(Instant.parse("2026-03-11T09:19:03Z"));
        given(startupState.getUptime()).willReturn(Duration.ofSeconds(20));
        given(dependencyStatusService.inspectDependencies()).willReturn(dependencies);

        mockMvc.perform(get("/ui-api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("ui-service"))
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.startupCompleted").value(true))
                .andExpect(jsonPath("$.dependencies.overallStatus").value("UP"))
                .andExpect(jsonPath("$.dependencies.postgres.available").value(true))
                .andExpect(jsonPath("$.dependencies.redis.available").value(true));
    }
}
