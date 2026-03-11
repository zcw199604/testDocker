package com.example.bizservice.model;

/**
 * Request body for creating a PostgreSQL note.
 *
 * @param title note title.
 * @param content note content.
 */
public record CreateNoteRequest(String title, String content) {
}
