package com.example.expensetracker.auth.dto;

import com.example.expensetracker.common.util.ValidationPatterns;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank @Email(regexp = ValidationPatterns.EMAIL, message = ValidationPatterns.EMAIL_MESSAGE) String email,
        @NotBlank @Pattern(regexp = "^\\d{4,8}$", message = "must be the numeric code from the email") String otp) {
}
