package com.example.bizservice.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bizservice.model.AutoscaleMetricsResponse;
import com.example.bizservice.service.AutoscaleMetricsService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for autoscale metrics endpoint.
 */
@WebMvcTest(AutoscaleMetricsController.class)
class AutoscaleMetricsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AutoscaleMetricsService autoscaleMetricsService;

    /**
     * Verifies the autoscale metrics payload is exposed to the control node.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldReturnAutoscaleMetricsPayload() throws Exception {
        AutoscaleMetricsResponse response = new AutoscaleMetricsResponse(
                "biz-service",
                "biz-service-1",
                "READY",
                true,
                2,
                18,
                1,
                0.3,
                42.5,
                Instant.parse("2026-03-11T05:00:00Z"),
                Instant.parse("2026-03-11T05:00:20Z"));
        given(autoscaleMetricsService.snapshot()).willReturn(response);

        mockMvc.perform(get("/api/autoscale/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("biz-service"))
                .andExpect(jsonPath("$.instanceId").value("biz-service-1"))
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.startupCompleted").value(true))
                .andExpect(jsonPath("$.inflightRequests").value(2))
                .andExpect(jsonPath("$.windowRequestCount").value(18))
                .andExpect(jsonPath("$.windowErrorCount").value(1))
                .andExpect(jsonPath("$.requestRatePerSecond").value(0.3))
                .andExpect(jsonPath("$.averageResponseTimeMs").value(42.5))
                .andExpect(jsonPath("$.windowStartedAt").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
