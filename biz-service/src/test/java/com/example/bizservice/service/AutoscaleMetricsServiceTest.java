package com.example.bizservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.example.bizservice.config.AutoscaleMetricsProperties;
import com.example.bizservice.model.AutoscaleMetricsResponse;
import com.example.bizservice.state.StartupState;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for autoscale metrics service.
 */
class AutoscaleMetricsServiceTest {

    /**
     * Verifies tracked requests are aggregated into the active metrics window.
     */
    @Test
    void shouldAggregateWindowMetricsForTrackedRequests() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-11T05:00:00Z"), ZoneId.of("UTC"));
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
                clock);

        autoscaleMetricsService.onRequestStarted();
        autoscaleMetricsService.onRequestFinished(Duration.ofMillis(100), 200, false);
        autoscaleMetricsService.onRequestStarted();
        autoscaleMetricsService.onRequestFinished(Duration.ofMillis(300), 503, false);

        AutoscaleMetricsResponse metricsResponse = autoscaleMetricsService.snapshot();

        assertThat(metricsResponse.serviceName()).isEqualTo("biz-service");
        assertThat(metricsResponse.instanceId()).isEqualTo("instance-1");
        assertThat(metricsResponse.status()).isEqualTo("READY");
        assertThat(metricsResponse.startupCompleted()).isTrue();
        assertThat(metricsResponse.inflightRequests()).isZero();
        assertThat(metricsResponse.windowRequestCount()).isEqualTo(2);
        assertThat(metricsResponse.windowErrorCount()).isEqualTo(1);
        assertThat(metricsResponse.requestRatePerSecond()).isEqualTo(2d / 60d);
        assertThat(metricsResponse.averageResponseTimeMs()).isEqualTo(200d);
        assertThat(metricsResponse.windowStartedAt()).isEqualTo(Instant.parse("2026-03-11T05:00:00Z"));
        assertThat(metricsResponse.timestamp()).isEqualTo(Instant.parse("2026-03-11T05:00:00Z"));
    }

    /**
     * Verifies the metrics window resets after the configured duration elapses.
     */
    @Test
    void shouldResetMetricsAfterWindowExpires() {
        MutableClock clock = new MutableClock(Instant.parse("2026-03-11T05:10:00Z"), ZoneId.of("UTC"));
        StartupState startupState = mock(StartupState.class);
        given(startupState.currentStatus()).willReturn("STARTING");
        given(startupState.isStartupCompleted()).willReturn(false);

        AutoscaleMetricsProperties properties = new AutoscaleMetricsProperties();
        properties.setWindowDuration(Duration.ofSeconds(30));
        AutoscaleMetricsService autoscaleMetricsService = new AutoscaleMetricsService(
                startupState,
                properties,
                "biz-service",
                "instance-2",
                clock);

        autoscaleMetricsService.onRequestStarted();
        autoscaleMetricsService.onRequestFinished(Duration.ofMillis(80), 200, false);
        clock.advance(Duration.ofSeconds(31));

        AutoscaleMetricsResponse metricsResponse = autoscaleMetricsService.snapshot();

        assertThat(metricsResponse.status()).isEqualTo("STARTING");
        assertThat(metricsResponse.startupCompleted()).isFalse();
        assertThat(metricsResponse.inflightRequests()).isZero();
        assertThat(metricsResponse.windowRequestCount()).isZero();
        assertThat(metricsResponse.windowErrorCount()).isZero();
        assertThat(metricsResponse.requestRatePerSecond()).isZero();
        assertThat(metricsResponse.averageResponseTimeMs()).isZero();
        assertThat(metricsResponse.windowStartedAt()).isEqualTo(Instant.parse("2026-03-11T05:10:31Z"));
        assertThat(metricsResponse.timestamp()).isEqualTo(Instant.parse("2026-03-11T05:10:31Z"));
    }

    /**
     * Verifies only business API paths are counted toward autoscale metrics.
     */
    @Test
    void shouldTrackOnlyBusinessApiPaths() {
        AutoscaleMetricsProperties properties = new AutoscaleMetricsProperties();
        properties.setWindowDuration(Duration.ofSeconds(60));
        AutoscaleMetricsService autoscaleMetricsService = new AutoscaleMetricsService(
                mock(StartupState.class),
                properties,
                "biz-service",
                "instance-3",
                Clock.systemUTC());

        assertThat(autoscaleMetricsService.shouldTrackPath("/api/test")).isTrue();
        assertThat(autoscaleMetricsService.shouldTrackPath("/api/pg/notes")).isTrue();
        assertThat(autoscaleMetricsService.shouldTrackPath("/api/health")).isFalse();
        assertThat(autoscaleMetricsService.shouldTrackPath("/api/dependencies")).isFalse();
        assertThat(autoscaleMetricsService.shouldTrackPath("/api/autoscale/metrics")).isFalse();
        assertThat(autoscaleMetricsService.shouldTrackPath("/actuator/health")).isFalse();
    }

    private static final class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zoneId;

        private MutableClock(Instant instant, ZoneId zoneId) {
            this.instant = instant;
            this.zoneId = zoneId;
        }

        /**
         * Advances the current clock instant.
         *
         * @param duration duration to add.
         */
        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zoneId;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
