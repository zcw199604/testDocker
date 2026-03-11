package com.example.uiservice.service;

import com.example.uiservice.model.CreateUiPreferenceRequest;
import com.example.uiservice.model.UiPreferenceResponse;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Handles real PostgreSQL read/write operations for UI preference data.
 */
@Service
public class UiPreferenceService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * Creates a UI preference service.
     *
     * @param jdbcTemplate JDBC template.
     */
    public UiPreferenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Creates or updates a UI preference record.
     *
     * @param request create request.
     * @return stored preference response.
     */
    public UiPreferenceResponse upsertPreference(CreateUiPreferenceRequest request) {
        validateRequest(request);
        List<UiPreferenceResponse> items = jdbcTemplate.query(
                """
                INSERT INTO ui_preferences (user_id, theme, language)
                VALUES (?, ?, ?)
                ON CONFLICT (user_id)
                DO UPDATE SET theme = EXCLUDED.theme,
                              language = EXCLUDED.language,
                              updated_at = CURRENT_TIMESTAMP
                RETURNING id, user_id, theme, language, created_at, updated_at
                """,
                (resultSet, rowNum) -> new UiPreferenceResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("user_id"),
                        resultSet.getString("theme"),
                        resultSet.getString("language"),
                        toInstant(resultSet.getTimestamp("created_at")),
                        toInstant(resultSet.getTimestamp("updated_at"))),
                request.userId().trim(),
                request.theme().trim(),
                request.language().trim());
        return items.get(0);
    }

    /**
     * Reads a UI preference by user id.
     *
     * @param userId user identifier.
     * @return preference response.
     */
    public UiPreferenceResponse getPreference(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank.");
        }
        List<UiPreferenceResponse> items = jdbcTemplate.query(
                "SELECT id, user_id, theme, language, created_at, updated_at FROM ui_preferences WHERE user_id = ?",
                (resultSet, rowNum) -> new UiPreferenceResponse(
                        resultSet.getLong("id"),
                        resultSet.getString("user_id"),
                        resultSet.getString("theme"),
                        resultSet.getString("language"),
                        toInstant(resultSet.getTimestamp("created_at")),
                        toInstant(resultSet.getTimestamp("updated_at"))),
                userId.trim());
        if (items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "UI preference not found for userId: " + userId);
        }
        return items.get(0);
    }

    private void validateRequest(CreateUiPreferenceRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required.");
        }
        if (request.userId() == null || request.userId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId must not be blank.");
        }
        if (request.theme() == null || request.theme().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "theme must not be blank.");
        }
        if (request.language() == null || request.language().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "language must not be blank.");
        }
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }
}
