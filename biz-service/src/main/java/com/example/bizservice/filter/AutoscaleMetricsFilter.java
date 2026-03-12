package com.example.bizservice.filter;

import com.example.bizservice.service.AutoscaleMetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Tracks business API traffic for autoscaling decisions.
 */
public class AutoscaleMetricsFilter extends OncePerRequestFilter {

    private final AutoscaleMetricsService autoscaleMetricsService;

    /**
     * Creates a request tracking filter.
     *
     * @param autoscaleMetricsService autoscale metrics service.
     */
    public AutoscaleMetricsFilter(AutoscaleMetricsService autoscaleMetricsService) {
        this.autoscaleMetricsService = autoscaleMetricsService;
    }

    /**
     * Skips duplicate async dispatch tracking.
     *
     * @return true because async redispatches should not be counted again.
     */
    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return true;
    }

    /**
     * Skips duplicate error dispatch tracking.
     *
     * @return true because error redispatches should not be counted again.
     */
    @Override
    protected boolean shouldNotFilterErrorDispatch() {
        return true;
    }

    /**
     * Filters tracked API requests and records response timings.
     *
     * @param request current request.
     * @param response current response.
     * @param filterChain remaining filter chain.
     * @throws ServletException on servlet failure.
     * @throws IOException on I/O failure.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        autoscaleMetricsService.onRequestStarted();
        long startedAtNanos = System.nanoTime();
        int responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        boolean failed = false;

        try {
            filterChain.doFilter(request, response);
            responseStatus = response.getStatus();
        } catch (IOException | ServletException | RuntimeException ex) {
            failed = true;
            throw ex;
        } finally {
            Duration responseTime = Duration.ofNanos(System.nanoTime() - startedAtNanos);
            autoscaleMetricsService.onRequestFinished(responseTime, responseStatus, failed);
        }
    }

    /**
     * Skips non-business endpoints so health polling does not affect scaling data.
     *
     * @param request current request.
     * @return true when the request should not be tracked.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !autoscaleMetricsService.shouldTrackPath(resolvePath(request));
    }

    private String resolvePath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        if (contextPath == null || contextPath.isBlank() || !requestUri.startsWith(contextPath)) {
            return requestUri;
        }
        return requestUri.substring(contextPath.length());
    }
}
