package com.example.expensetracker.auth.dto;

import com.example.expensetracker.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(@NotBlank @Email(regexp = ValidationPatterns.EMAIL, message = ValidationPatterns.EMAIL_MESSAGE) String email) {
}
