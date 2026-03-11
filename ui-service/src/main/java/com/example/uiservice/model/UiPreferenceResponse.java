package com.example.uiservice.model;

import java.time.Instant;

/**
 * Response body for a UI preference record.
 *
 * @param id record identifier.
 * @param userId frontend user identifier.
 * @param theme preferred theme.
 * @param language preferred language.
 * @param createdAt creation timestamp.
 * @param updatedAt last update timestamp.
 */
public record UiPreferenceResponse(
        Long id,
        String userId,
        String theme,
        String language,
        Instant createdAt,
        Instant updatedAt) {
}
