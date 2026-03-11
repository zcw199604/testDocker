package com.example.uiservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.uiservice.model.UiPreferenceResponse;
import com.example.uiservice.service.UiPreferenceService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for UI preference endpoints.
 */
@WebMvcTest(UiPreferenceController.class)
class UiPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UiPreferenceService uiPreferenceService;

    /**
     * Verifies a UI preference can be created.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldCreateUiPreference() throws Exception {
        UiPreferenceResponse response = new UiPreferenceResponse(
                1L,
                "user-1",
                "dark",
                "zh-CN",
                Instant.parse("2026-03-11T09:21:00Z"),
                Instant.parse("2026-03-11T09:21:00Z"));
        given(uiPreferenceService.upsertPreference(any())).willReturn(response);

        mockMvc.perform(post("/ui-api/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId":"user-1","theme":"dark","language":"zh-CN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.theme").value("dark"));
    }

    /**
     * Verifies a UI preference can be queried.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldGetUiPreference() throws Exception {
        UiPreferenceResponse response = new UiPreferenceResponse(
                1L,
                "user-1",
                "dark",
                "zh-CN",
                Instant.parse("2026-03-11T09:21:00Z"),
                Instant.parse("2026-03-11T09:21:05Z"));
        given(uiPreferenceService.getPreference("user-1")).willReturn(response);

        mockMvc.perform(get("/ui-api/preferences/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.language").value("zh-CN"));
    }
}
