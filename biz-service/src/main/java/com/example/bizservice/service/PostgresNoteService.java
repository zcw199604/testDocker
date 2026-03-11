package com.example.bizservice.service;

import com.example.bizservice.model.CreateNoteRequest;
import com.example.bizservice.model.NoteResponse;
import com.example.bizservice.model.NotesResponse;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles real PostgreSQL read/write operations for notes.
 */
@Service
public class PostgresNoteService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a PostgreSQL note service.
     *
     * @param jdbcTemplate JDBC template.
     */
    public PostgresNoteService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates a new note in PostgreSQL.
     *
     * @param request create request.
     * @return created note.
     */
    public NoteResponse createNote(CreateNoteRequest request) {
        validateCreateRequest(request);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO notes (title, content) VALUES (?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, request.title().trim());
            statement.setString(2, request.content().trim());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKeys() == null
                ? null
                : (Number) keyHolder.getKeys().get("id");
        if (key == null) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create note.");
        }
        return getNoteById(key.longValue());
    }

    /**
     * Reads a note by id.
     *
     * @param id note id.
     * @return note data.
     */
    public NoteResponse getNoteById(Long id) {
        List<NoteResponse> items = jdbcTemplate.query(
                "SELECT id, title, content, created_at FROM notes WHERE id = ?",
                (resultSet, rowNum) -> new NoteResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        toInstant(resultSet.getTimestamp("created_at"))),
                id);
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found: " + id);
        }
        return items.get(0);
    }

    /**
     * Lists notes from PostgreSQL.
     *
     * @return note list response.
     */
    public NotesResponse listNotes() {
        List<NoteResponse> items = jdbcTemplate.query(
                "SELECT id, title, content, created_at FROM notes ORDER BY id DESC",
                (resultSet, rowNum) -> new NoteResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("title"),
                        resultSet.getString("content"),
                        toInstant(resultSet.getTimestamp("created_at"))));
        return new NotesResponse(items.size(), List.copyOf(items), Instant.now());
    }

    private void validateCreateRequest(CreateNoteRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "title must not be blank.");
        }
        if (request.content() == null || request.content().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank.");
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}
