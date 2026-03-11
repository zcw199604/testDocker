package com.example.uiservice.controller;

import com.example.uiservice.model.PutUiSessionRequest;
import com.example.uiservice.model.UiSessionResponse;
import com.example.uiservice.service.UiSessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes real Redis read/write endpoints for UI session data.
 */
@RestController
@RequestMapping("/ui-api/sessions")
public class UiSessionController {

    private final UiSessionService uiSessionService;

    /**
     * Creates a UI session controller.
     *
     * @param uiSessionService UI session service.
     */
    public UiSessionController(UiSessionService uiSessionService) {
        this.uiSessionService = uiSessionService;
    }

    /**
     * Stores a UI session in Redis.
     *
     * @param request put request.
     * @return stored session response.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UiSessionResponse put(@RequestBody PutUiSessionRequest request) {
        return uiSessionService.putSession(request);
    }

    /**
     * Reads a UI session from Redis.
     *
     * @param sessionId session identifier.
     * @return session response.
     */
    @GetMapping("/{sessionId}")
    public UiSessionResponse get(@PathVariable String sessionId) {
        return uiSessionService.getSession(sessionId);
    }
}
