package com.example.expensetracker.auth.dto;

import com.example.expensetracker.auth.OtpPurpose;
import com.example.expensetracker.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** {@code purpose} is optional, default EMAIL_VERIFICATION. */
public record ResendOtpRequest(@NotBlank @Email(regexp = ValidationPatterns.EMAIL, message = ValidationPatterns.EMAIL_MESSAGE) String email, OtpPurpose purpose) {
}
