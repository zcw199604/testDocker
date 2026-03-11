package com.example.uiservice.controller;

import com.example.uiservice.model.RootResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the default ui-service entry endpoint.
 */
@RestController
public class RootController {

    private final String applicationName;

    /**
     * Creates a root controller.
     *
     * @param applicationName configured Spring application name.
     */
    public RootController(@Value("${spring.application.name:ui-service}") String applicationName) {
        this.applicationName = applicationName;
    }

    /**
     * Returns a summary of ui-service routes and responsibilities.
     *
     * @return root response.
     */
    @GetMapping("/")
    public RootResponse root() {
        return new RootResponse(
                applicationName,
                "frontend-interaction-service",
                List.of(
                        "/ui-api/health",
                        "/ui-api/dependencies",
                        "/ui-api/test",
                        "/ui-api/preferences",
                        "/ui-api/sessions"),
                Instant.now());
    }
}
