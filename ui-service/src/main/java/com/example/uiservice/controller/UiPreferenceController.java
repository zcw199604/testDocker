package com.example.uiservice.controller;

import com.example.uiservice.model.CreateUiPreferenceRequest;
import com.example.uiservice.model.UiPreferenceResponse;
import com.example.uiservice.service.UiPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes real PostgreSQL read/write endpoints for UI preference data.
 */
@RestController
@RequestMapping("/ui-api/preferences")
public class UiPreferenceController {

    private final UiPreferenceService uiPreferenceService;

    /**
     * Creates a UI preference controller.
     *
     * @param uiPreferenceService UI preference service.
     */
    public UiPreferenceController(UiPreferenceService uiPreferenceService) {
        this.uiPreferenceService = uiPreferenceService;
    }

    /**
     * Creates or updates a UI preference record.
     *
     * @param request create request.
     * @return stored preference.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UiPreferenceResponse create(@RequestBody CreateUiPreferenceRequest request) {
        return uiPreferenceService.upsertPreference(request);
    }

    /**
     * Reads a UI preference by user id.
     *
     * @param userId frontend user identifier.
     * @return preference response.
     */
    @GetMapping("/{userId}")
    public UiPreferenceResponse getByUserId(@PathVariable String userId) {
        return uiPreferenceService.getPreference(userId);
    }
}
