package com.example.expensetracker.common.security;

import java.util.UUID;

/**
 * The authenticated principal, built from the access token.
 * Inject in controllers with {@code @AuthenticationPrincipal CurrentUser user}.
 */
public record CurrentUser(UUID id, String email) {
}
