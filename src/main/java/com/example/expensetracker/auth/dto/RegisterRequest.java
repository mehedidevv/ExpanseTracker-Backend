package com.example.expensetracker.auth.dto;

import com.example.expensetracker.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 255)
        @Email(regexp = ValidationPatterns.EMAIL, message = ValidationPatterns.EMAIL_MESSAGE) String email,
        @NotBlank @Size(min = 8, max = 72) String password,
        @NotBlank @Size(max = 100) String fullName,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO 4217 code") String currencyCode) {
}
