package com.example.bizservice.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.bizservice.model.NoteResponse;
import com.example.bizservice.model.NotesResponse;
import com.example.bizservice.service.PostgresNoteService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Web tests for PostgreSQL note endpoints.
 */
@WebMvcTest(PostgresNoteController.class)
class PostgresNoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostgresNoteService postgresNoteService;

    /**
     * Verifies a PostgreSQL note can be created.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldCreateNote() throws Exception {
        NoteResponse response = new NoteResponse(1L, "hello", "world", Instant.parse("2026-03-11T05:45:00Z"));
        given(postgresNoteService.createNote(any())).willReturn(response);

        mockMvc.perform(post("/api/pg/notes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"hello","content":"world"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("hello"))
                .andExpect(jsonPath("$.content").value("world"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    /**
     * Verifies PostgreSQL notes can be listed.
     *
     * @throws Exception when the request fails.
     */
    @Test
    void shouldListNotes() throws Exception {
        NotesResponse response = new NotesResponse(
                1,
                List.of(new NoteResponse(1L, "hello", "world", Instant.parse("2026-03-11T05:45:00Z"))),
                Instant.parse("2026-03-11T05:45:01Z"));
        given(postgresNoteService.listNotes()).willReturn(response);

        mockMvc.perform(get("/api/pg/notes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1))
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[0].title").value("hello"));
    }
}
