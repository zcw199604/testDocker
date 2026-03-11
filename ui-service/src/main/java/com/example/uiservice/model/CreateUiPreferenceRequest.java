package com.example.uiservice.model;

/**
 * Request body for creating or updating a UI preference record.
 *
 * @param userId frontend user identifier.
 * @param theme preferred theme.
 * @param language preferred language.
 */
public record CreateUiPreferenceRequest(String userId, String theme, String language) {
}
