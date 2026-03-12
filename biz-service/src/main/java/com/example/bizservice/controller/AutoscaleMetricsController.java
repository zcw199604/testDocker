package com.example.bizservice.controller;

import com.example.bizservice.model.AutoscaleMetricsResponse;
import com.example.bizservice.service.AutoscaleMetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes internal rolling metrics for autoscale decisions.
 */
@RestController
@RequestMapping("/api/autoscale")
public class AutoscaleMetricsController {

    private final AutoscaleMetricsService autoscaleMetricsService;

    /**
     * Creates an autoscale metrics controller.
     *
     * @param autoscaleMetricsService autoscale metrics service.
     */
    public AutoscaleMetricsController(AutoscaleMetricsService autoscaleMetricsService) {
        this.autoscaleMetricsService = autoscaleMetricsService;
    }

    /**
     * Returns a rolling autoscale metrics snapshot.
     *
     * @return metrics payload for the control node.
     */
    @GetMapping("/metrics")
    public AutoscaleMetricsResponse metrics() {
        return autoscaleMetricsService.snapshot();
    }
}
