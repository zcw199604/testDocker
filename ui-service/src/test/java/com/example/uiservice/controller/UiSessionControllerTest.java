package com.example.uiservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.uiservice.model.UiSessionResponse;
import com.example.uiservice.service.UiSessionService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for UI session endpoints.
 */
@WebMvcTest(UiSessionController.class)
class UiSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UiSessionService uiSessionService;

    /**
     * Verifies a UI session can be stored.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldStoreUiSession() throws Exception {
        UiSessionResponse response = new UiSessionResponse(
                "ui:session:s-1",
                "user-1",
                "/dashboard",
                "{\"userId\":\"user-1\",\"page\":\"/dashboard\"}",
                true,
                300L,
                Instant.parse("2026-03-11T09:22:00Z"));
        given(uiSessionService.putSession(any())).willReturn(response);

        mockMvc.perform(post("/ui-api/sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sessionId":"s-1","userId":"user-1","page":"/dashboard","ttlSeconds":300}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").value("ui:session:s-1"))
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.ttlSeconds").value(300));
    }

    /**
     * Verifies a UI session can be queried.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldGetUiSession() throws Exception {
        UiSessionResponse response = new UiSessionResponse(
                "ui:session:s-1",
                "user-1",
                "/dashboard",
                "{\"userId\":\"user-1\",\"page\":\"/dashboard\"}",
                true,
                300L,
                Instant.parse("2026-03-11T09:22:05Z"));
        given(uiSessionService.getSession("s-1")).willReturn(response);

        mockMvc.perform(get("/ui-api/sessions/s-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("ui:session:s-1"))
                .andExpect(jsonPath("$.page").value("/dashboard"));
    }
}
