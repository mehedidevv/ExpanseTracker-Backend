package com.example.expensetracker.user;

import com.example.expensetracker.auth.RefreshTokenRepository;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.common.exception.ResourceNotFoundException;
import com.example.expensetracker.user.dto.ChangePasswordRequest;
import com.example.expensetracker.user.dto.UpdateProfileRequest;
import com.example.expensetracker.user.dto.UserResponse;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-U1..U3. */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(UUID userId) {
        return userMapper.toResponse(load(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest req) {
        User user = load(userId);
        user.setFullName(req.fullName().trim());
        user.setCurrencyCode(req.currencyCode());
        return userMapper.toResponse(user);
    }

    /** Changing the password also signs the user out everywhere (all refresh tokens revoked). */
    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest req) {
        User user = load(userId);
        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        refreshTokenRepository.revokeAllForUser(userId);
    }

    /** Used by other features to attach the owner to new records. */
    @Transactional(readOnly = true)
    public User getReference(UUID userId) {
        return load(userId);
    }

    private User load(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }
}
