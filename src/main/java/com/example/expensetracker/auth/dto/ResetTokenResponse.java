package com.example.expensetracker.auth.dto;

/** Returned by verify-reset-otp; send {@code resetToken} to reset-password. */
public record ResetTokenResponse(String resetToken, long expiresIn) {
}
