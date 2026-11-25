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
import com.example.expensetracker.common.exception.ApiException;
import com.example.expensetracker.common.exception.ConflictException;
import com.example.expensetracker.common.exception.ErrorCode;
import com.example.expensetracker.common.exception.UnauthorizedException;
import com.example.expensetracker.common.security.JwtProperties;
import com.example.expensetracker.common.security.JwtService;
import com.example.expensetracker.common.util.HashUtils;
import com.example.expensetracker.user.User;
import com.example.expensetracker.user.UserRepository;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FR-A1..A4 + email OTP.
 * <ul>
 *   <li>Register → OTP email → verify-email → account created + tokens. Before that only a
 *       pending registration exists (see {@link RegistrationService}).</li>
 *   <li>Forgot password → OTP email → verify-reset-otp → reset token → reset-password.</li>
 *   <li>Access token = short-lived JWT. Refresh token = random opaque string, stored only as
 *       SHA-256 hash, rotated on every use.</li>
 * </ul>
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int REFRESH_TOKEN_BYTES = 48;
    private static final String NO_ACCOUNT = "No account found with this email";
    private static final String WRONG_PASSWORD = "Incorrect password";
    private static final String INVALID_REFRESH = "Invalid or expired refresh token";

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final OtpService otpService;
    private final RegistrationService registrationService;

    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties,
                       OtpService otpService, RegistrationService registrationService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.otpService = otpService;
        this.registrationService = registrationService;
    }

    /**
     * Emails a verification code. No account exists until {@link #verifyEmail} succeeds; registering
     * again before that just replaces the pending details and sends a new code.
     */
    @Transactional
    public MessageResponse register(RegisterRequest req) {
        String email = normalizeEmail(req.email());
        registrationService.start(email, req);
        return new MessageResponse("Verification code sent to " + email);
    }

    /** Verifies the registration code, creates the account and logs the user in. */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse verifyEmail(VerifyOtpRequest req) {
        User user = registrationService.verify(normalizeEmail(req.email()), req.otp());
        return issueTokens(user);
    }

    /** Sends a new code: sign-up verification (default) or password reset. */
    @Transactional
    public MessageResponse resendOtp(ResendOtpRequest req) {
        if (req.purpose() == OtpPurpose.PASSWORD_RESET) {
            return forgotPassword(new ForgotPasswordRequest(req.email()));
        }
        String email = normalizeEmail(req.email());
        registrationService.resend(email);
        return new MessageResponse("Verification code sent to " + email);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest req) {
        String email = normalizeEmail(req.email());
        User user = userRepository.findByEmail(email).orElseThrow(() -> accountMissing(email));
        otpService.issue(user, OtpPurpose.PASSWORD_RESET);
        return new MessageResponse("Password reset code sent to " + email);
    }

    /** Checks the password-reset code and returns a single-use reset token. */
    @Transactional(noRollbackFor = ApiException.class)
    public ResetTokenResponse verifyResetOtp(VerifyOtpRequest req) {
        String email = normalizeEmail(req.email());
        User user = userRepository.findByEmail(email).orElseThrow(() -> accountMissing(email));
        OtpCode otp = otpService.verify(user, OtpPurpose.PASSWORD_RESET, req.otp());
        return new ResetTokenResponse(otpService.issueResetToken(otp), otpService.resetTokenTtlSeconds());
    }

    /** Sets the new password and signs the user out everywhere. */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        User user = otpService.consumeResetToken(req.resetToken());
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setEmailVerified(true); // the code proved they own the inbox
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("Password reset: {}", user.getId());
    }

    /**
     * Only verified accounts can log in. If the email only has a pending sign-up and the password is
     * right, a fresh code is emailed (cooldown permitting) and 403 EMAIL_NOT_VERIFIED is returned.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public AuthResponse login(LoginRequest req) {
        String email = normalizeEmail(req.email());
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            PendingRegistration pending = registrationService.findActive(email)
                    .orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, NO_ACCOUNT));
            if (!registrationService.passwordMatches(pending, req.password())) {
                throw new UnauthorizedException(WRONG_PASSWORD);
            }
            registrationService.resendQuietly(email);
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Email is not verified. Enter the code sent to your email to finish registration");
        }
        if (!passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException(WRONG_PASSWORD);
        }
        return issueTokens(user);
    }

    /** 403 if the email is still a pending sign-up, else 404. */
    private ApiException accountMissing(String email) {
        if (registrationService.findActive(email).isPresent()) {
            return new ApiException(ErrorCode.EMAIL_NOT_VERIFIED,
                    "Email is not verified. Finish registration with the code sent to your email first");
        }
        return new ApiException(ErrorCode.NOT_FOUND, NO_ACCOUNT);
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public AuthResponse refresh(RefreshRequest req) {
        RefreshToken token = refreshTokenRepository.findByTokenHash(HashUtils.sha256Hex(req.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException(INVALID_REFRESH));

        if (token.isRevoked()) {
            // A revoked token was re-used: assume theft, kill every session of this user.
            refreshTokenRepository.revokeAllForUser(token.getUser().getId());
            log.warn("Refresh token re-use detected for user {}", token.getUser().getId());
            throw new UnauthorizedException(INVALID_REFRESH);
        }
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException(INVALID_REFRESH);
        }
        token.setRevoked(true);
        return issueTokens(token.getUser());
    }

    /** Revokes the given refresh token if it belongs to the caller. Idempotent. */
    @Transactional
    public void logout(UUID userId, RefreshRequest req) {
        refreshTokenRepository.findByTokenHash(HashUtils.sha256Hex(req.refreshToken()))
                .filter(t -> t.getUser().getId().equals(userId))
                .ifPresent(t -> t.setRevoked(true));
    }

    private AuthResponse issueTokens(User user) {
        String rawRefresh = HashUtils.randomToken(REFRESH_TOKEN_BYTES);
        refreshTokenRepository.save(new RefreshToken(user, HashUtils.sha256Hex(rawRefresh),
                Instant.now().plus(jwtProperties.refreshTokenTtl())));
        String access = jwtService.generateAccessToken(user.getId(), user.getEmail());
        return new AuthResponse(access, rawRefresh, "Bearer", jwtService.accessTokenTtlSeconds());
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
