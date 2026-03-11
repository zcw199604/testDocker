package com.example.bizservice.model;

import java.time.Instant;

/**
 * Response body for a PostgreSQL note.
 *
 * @param id note identifier.
 * @param title note title.
 * @param content note content.
 * @param createdAt creation timestamp.
 */
public record NoteResponse(Long id, String title, String content, Instant createdAt) {
}
