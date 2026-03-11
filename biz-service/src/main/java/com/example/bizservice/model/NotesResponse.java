package com.example.bizservice.model;

import java.time.Instant;
import java.util.List;

/**
 * Response body for listing PostgreSQL notes.
 *
 * @param count number of returned notes.
 * @param items note items.
 * @param timestamp response generation timestamp.
 */
public record NotesResponse(int count, List<NoteResponse> items, Instant timestamp) {
}
