package com.example.bizservice.controller;

import com.example.bizservice.model.CreateNoteRequest;
import com.example.bizservice.model.NoteResponse;
import com.example.bizservice.model.NotesResponse;
import com.example.bizservice.service.PostgresNoteService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes real PostgreSQL note read/write endpoints.
 */
@RestController
@RequestMapping("/api/pg/notes")
public class PostgresNoteController {

    private final PostgresNoteService postgresNoteService;

    /**
     * Creates a PostgreSQL notes controller.
     *
     * @param postgresNoteService PostgreSQL note service.
     */
    public PostgresNoteController(PostgresNoteService postgresNoteService) {
        this.postgresNoteService = postgresNoteService;
    }

    /**
     * Creates a new note.
     *
     * @param request create request.
     * @return created note.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse create(@RequestBody CreateNoteRequest request) {
        return postgresNoteService.createNote(request);
    }

    /**
     * Reads a note by id.
     *
     * @param id note id.
     * @return note response.
     */
    @GetMapping("/{id}")
    public NoteResponse getById(@PathVariable Long id) {
        return postgresNoteService.getNoteById(id);
    }

    /**
     * Lists notes.
     *
     * @return notes response.
     */
    @GetMapping
    public NotesResponse list() {
        return postgresNoteService.listNotes();
    }
}
