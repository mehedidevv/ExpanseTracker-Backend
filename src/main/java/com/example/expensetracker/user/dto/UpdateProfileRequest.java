package com.example.expensetracker.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 4217 code") String currencyCode) {
}
