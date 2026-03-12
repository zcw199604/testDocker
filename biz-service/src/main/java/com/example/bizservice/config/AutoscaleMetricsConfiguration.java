package com.example.bizservice.config;

import com.example.bizservice.filter.AutoscaleMetricsFilter;
import com.example.bizservice.service.AutoscaleMetricsService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers autoscale metrics infrastructure for business API requests.
 */
@Configuration
public class AutoscaleMetricsConfiguration {

    /**
     * Creates the autoscale metrics filter bean.
     *
     * @param autoscaleMetricsService autoscale metrics service.
     * @return filter instance.
     */
    @Bean
    public AutoscaleMetricsFilter autoscaleMetricsFilter(AutoscaleMetricsService autoscaleMetricsService) {
        return new AutoscaleMetricsFilter(autoscaleMetricsService);
    }

    /**
     * Registers the autoscale metrics filter only for API routes.
     *
     * @param autoscaleMetricsFilter filter instance.
     * @return filter registration.
     */
    @Bean
    public FilterRegistrationBean<AutoscaleMetricsFilter> autoscaleMetricsFilterRegistration(
            AutoscaleMetricsFilter autoscaleMetricsFilter) {
        FilterRegistrationBean<AutoscaleMetricsFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(autoscaleMetricsFilter);
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE + 50);
        return registrationBean;
    }
}
