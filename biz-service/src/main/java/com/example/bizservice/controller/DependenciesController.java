package com.example.bizservice.controller;

import com.example.bizservice.model.DependenciesResponse;
import com.example.bizservice.service.DependencyStatusService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes dependency validation endpoints.
 */
@RestController
@RequestMapping("/api")
public class DependenciesController {

    private final DependencyStatusService dependencyStatusService;

    /**
     * Creates a controller for dependency validation.
     *
     * @param dependencyStatusService dependency validation service.
     */
    public DependenciesController(DependencyStatusService dependencyStatusService) {
        this.dependencyStatusService = dependencyStatusService;
    }

    /**
     * Returns PostgreSQL and Redis availability information.
     *
     * @return dependencies response.
     */
    @GetMapping("/dependencies")
    public DependenciesResponse dependencies() {
        return dependencyStatusService.inspectDependencies();
    }
}
