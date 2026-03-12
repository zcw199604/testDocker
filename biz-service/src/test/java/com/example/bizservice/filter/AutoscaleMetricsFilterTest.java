package com.example.bizservice.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bizservice.config.AutoscaleMetricsProperties;
import com.example.bizservice.model.AutoscaleMetricsResponse;
import com.example.bizservice.service.AutoscaleMetricsService;
import com.example.bizservice.state.StartupState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Tests for autoscale request filtering.
 */
class AutoscaleMetricsFilterTest {

    /**
     * Verifies tracked business requests are counted while excluded endpoints are ignored.
     *
     * @throws Exception when mock requests fail.
     */
    @Test
    void shouldCountOnlyTrackedBusinessRequests() throws Exception {
        StartupState startupState = mock(StartupState.class);
        given(startupState.currentStatus()).willReturn("READY");
        given(startupState.isStartupCompleted()).willReturn(true);

        AutoscaleMetricsProperties properties = new AutoscaleMetricsProperties();
        properties.setWindowDuration(Duration.ofSeconds(60));
        AutoscaleMetricsService autoscaleMetricsService = new AutoscaleMetricsService(
                startupState,
                properties,
                "biz-service",
                "instance-1",
                Clock.fixed(Instant.parse("2026-03-11T05:20:00Z"), ZoneId.of("UTC")));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new SampleController())
                .addFilters(new AutoscaleMetricsFilter(autoscaleMetricsService))
                .build();

        mockMvc.perform(get("/api/test"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/autoscale/metrics"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/fail"))
                .andExpect(status().isInternalServerError());

        AutoscaleMetricsResponse metricsResponse = autoscaleMetricsService.snapshot();

        assertThat(metricsResponse.windowRequestCount()).isEqualTo(2);
        assertThat(metricsResponse.windowErrorCount()).isEqualTo(1);
        assertThat(metricsResponse.inflightRequests()).isZero();
    }

    @RestController
    private static final class SampleController {

        /**
         * Returns a sample business endpoint payload.
         *
         * @return simple response body.
         */
        @GetMapping("/api/test")
        public Map<String, Object> test() {
            return Map.of("ok", true);
        }

        /**
         * Returns a sample excluded health payload.
         *
         * @return simple response body.
         */
        @GetMapping("/api/health")
        public Map<String, Object> health() {
            return Map.of("status", "UP");
        }

        /**
         * Returns a sample excluded autoscale payload.
         *
         * @return simple response body.
         */
        @GetMapping("/api/autoscale/metrics")
        public Map<String, Object> autoscaleMetrics() {
            return Map.of("ignored", true);
        }

        /**
         * Returns a sample failing business response.
         *
         * @return failing response entity.
         */
        @GetMapping("/api/fail")
        public ResponseEntity<Void> fail() {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
