package com.example.expensetracker.auth;

import com.example.expensetracker.auth.dto.AuthResponse;
import com.example.expensetracker.auth.dto.ForgotPasswordRequest;
import com.example.expensetracker.auth.dto.LoginRequest;
import com.example.expensetracker.auth.dto.MessageResponse;
import com.example.expensetracker.auth.dto.RefreshRequest;
import com.example.expensetracker.auth.dto.RegisterRequest;
import com.example.expensetracker.auth.dto.ResendOtpRequest;
import com.example.expensetracker.auth.dto.ResetPasswordRequest;
import com.example.expensetracker.auth.dto.ResetTokenResponse;
import com.example.expensetracker.auth.dto.VerifyOtpRequest;
import com.example.expensetracker.common.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.example.expensetracker.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Register a new user and email a verification code")
    @SecurityRequirements
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.created(authService.register(request).message(), null);
    }

    @Operation(summary = "Verify the registration code; returns tokens")
    @SecurityRequirements
    @PostMapping("/verify-email")
    public ApiResponse<AuthResponse> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok("Email verified successfully. Your account is ready", authService.verifyEmail(request));
    }

    @Operation(summary = "Send a new code (purpose EMAIL_VERIFICATION by default, or PASSWORD_RESET)")
    @SecurityRequirements
    @PostMapping("/resend-otp")
    public ApiResponse<Void> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        return ApiResponse.ok(authService.resendOtp(request).message());
    }

    @Operation(summary = "Email a password-reset code")
    @SecurityRequirements
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        return ApiResponse.ok(authService.forgotPassword(request).message());
    }

    @Operation(summary = "Verify the password-reset code; returns a single-use reset token")
    @SecurityRequirements
    @PostMapping("/verify-reset-otp")
    public ApiResponse<ResetTokenResponse> verifyResetOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.ok("Code verified. Use the reset token to set a new password", authService.verifyResetOtp(request));
    }

    @Operation(summary = "Set a new password with the reset token (signs out all sessions)")
    @SecurityRequirements
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok("Password reset successfully. Please log in with your new password");
    }

    @Operation(summary = "Login with email and password")
    @SecurityRequirements
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Login successful", authService.login(request));
    }

    @Operation(summary = "Exchange a refresh token for a new token pair (rotation)")
    @SecurityRequirements
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.ok("Token refreshed successfully", authService.refresh(request));
    }

    @Operation(summary = "Revoke a refresh token")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal CurrentUser user, @Valid @RequestBody RefreshRequest request) {
        authService.logout(user.id(), request);
        return ApiResponse.ok("Logged out successfully");
    }
}
