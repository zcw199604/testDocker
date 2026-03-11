package com.example.uiservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for the ui-service root endpoint.
 */
@WebMvcTest(RootController.class)
class RootControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies the root endpoint returns service summary data.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldReturnRootSummary() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceName").value("ui-service"))
                .andExpect(jsonPath("$.role").value("frontend-interaction-service"))
                .andExpect(jsonPath("$.availableRoutes[0]").value("/ui-api/health"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
