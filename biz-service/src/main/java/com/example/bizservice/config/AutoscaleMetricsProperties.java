package com.example.bizservice.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for autoscale metric collection.
 */
@Component
@ConfigurationProperties(prefix = "autoscale.metrics")
public class AutoscaleMetricsProperties {

    private Duration windowDuration = Duration.ofSeconds(60);

    /**
     * Returns the active autoscale metrics window duration.
     *
     * @return positive window duration.
     */
    public Duration getWindowDuration() {
        return windowDuration;
    }

    /**
     * Sets the active autoscale metrics window duration.
     *
     * @param windowDuration configured window duration.
     */
    public void setWindowDuration(Duration windowDuration) {
        if (windowDuration == null || windowDuration.isZero() || windowDuration.isNegative()) {
            this.windowDuration = Duration.ofSeconds(60);
            return;
        }
        this.windowDuration = windowDuration;
    }
}
