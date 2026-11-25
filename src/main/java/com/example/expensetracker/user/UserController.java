package com.example.expensetracker.user;

import com.example.expensetracker.common.security.CurrentUser;
import com.example.expensetracker.user.dto.ChangePasswordRequest;
import com.example.expensetracker.user.dto.UpdateProfileRequest;
import com.example.expensetracker.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import com.example.expensetracker.common.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User")
@RestController
@RequestMapping("/api/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Get the current user's profile")
    @GetMapping
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal CurrentUser user) {
        return ApiResponse.ok("Profile fetched successfully", userService.getProfile(user.id()));
    }

    @Operation(summary = "Update full name and currency")
    @PutMapping
    public ApiResponse<UserResponse> update(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok("Profile updated successfully", userService.updateProfile(user.id(), request));
    }

    @Operation(summary = "Change password (signs out all sessions)")
    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CurrentUser user,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(user.id(), request);
        return ApiResponse.ok("Password changed successfully. Please log in again on your other devices");
    }
}
