package com.example.uiservice.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for the ui-service sample endpoint.
 */
@WebMvcTest(TestController.class)
class TestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    /**
     * Verifies the sample endpoint returns the expected payload.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldReturnSampleResponse() throws Exception {
        mockMvc.perform(get("/ui-api/test").param("name", "frontend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.endpoint").value("ui-test"))
                .andExpect(jsonPath("$.requestedName").value("frontend"))
                .andExpect(jsonPath("$.message").value("Hello, frontend! ui-service is responding normally."))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
