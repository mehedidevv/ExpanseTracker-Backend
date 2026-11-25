package com.example.expensetracker.user.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, String fullName, String currencyCode, Instant createdAt) {
}
