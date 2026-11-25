package com.example.expensetracker.auth;

import com.example.expensetracker.auth.dto.RegisterRequest;
import com.example.expensetracker.common.exception.ApiException;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.common.exception.ConflictException;
import com.example.expensetracker.common.mail.EmailDomainChecker;
import com.example.expensetracker.user.User;
import com.example.expensetracker.user.UserRepository;
import com.example.expensetracker.common.exception.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Sign-up with email OTP. The details wait in {@code pending_registrations}; the {@code users} row is
 * created only when the code is verified, so a wrong or fake email never becomes an account.
 * Callers own the transaction.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private final PendingRegistrationRepository pendingRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final EmailDomainChecker domainChecker;

    public RegistrationService(PendingRegistrationRepository pendingRepository, UserRepository userRepository,
                               PasswordEncoder passwordEncoder, OtpService otpService,
                               EmailDomainChecker domainChecker) {
        this.pendingRepository = pendingRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.domainChecker = domainChecker;
    }

    /** Validates, emails a code, then stores/updates the pending sign-up. Nothing is saved if the email fails. */
    public void start(String email, RegisterRequest req) {
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }
        if (!domainChecker.canReceiveMail(email)) {
            throw new BadRequestException("This email address cannot receive mail. Check it for typos");
        }
        Instant now = Instant.now();
        // A stale (expired) sign-up is simply reused/overwritten, without cooldown.
        PendingRegistration pending = pendingRepository.findByEmail(email).orElse(null);
        if (pending != null && !isStale(pending, now)) {
            otpService.checkCooldown(pending.getLastSentAt(), now);
        }

        String code = otpService.newCode();
        otpService.sendCode(email, OtpPurpose.EMAIL_VERIFICATION, code);

        if (pending == null) {
            pending = new PendingRegistration(email);
        }
        pending.setPassword(passwordEncoder.encode(req.password()));
        pending.setFullName(req.fullName().trim());
        pending.setCurrencyCode(req.currencyCode());
        pending.newCode(OtpService.hashCode(email, code), now.plus(otpService.props().ttl()), now);
        pendingRepository.save(pending);
    }

    /**
     * Checks the code and creates the real, verified user. A wrong code counts an attempt, so the
     * caller's transaction must not roll back on {@link ApiException}.
     */
    public User verify(String email, String code) {
        Instant now = Instant.now();
        PendingRegistration pending = findActive(email).orElseThrow(() -> noPendingError(email));
        if (!pending.getExpiresAt().isAfter(now)) {
            throw new BadRequestException(OtpService.CODE_EXPIRED);
        }
        if (!OtpService.codeMatches(pending.getCodeHash(), email, code)) {
            pending.recordFailedAttempt();
            int left = otpService.props().maxAttempts() - pending.getAttempts();
            if (left <= 0) {
                pending.newCode(pending.getCodeHash(), now, pending.getLastSentAt()); // block this code
                throw new ApiException(ErrorCode.TOO_MANY_REQUESTS, OtpService.TOO_MANY_ATTEMPTS);
            }
            throw new BadRequestException(OtpService.wrongCode(left));
        }

        pendingRepository.delete(pending);
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("This email is already verified. Please log in");
        }
        User user = new User(email, pending.getPassword(), pending.getFullName(), pending.getCurrencyCode());
        user.setEmailVerified(true);
        user = userRepository.save(user);
        log.info("User registered and verified: {}", user.getId());
        return user;
    }

    /** New code for a pending sign-up. 409 if already verified, 404 if there is no (unexpired) sign-up. */
    public void resend(String email) {
        PendingRegistration pending = findActive(email).orElseThrow(() -> noPendingError(email));
        Instant now = Instant.now();
        otpService.checkCooldown(pending.getLastSentAt(), now);
        String code = otpService.newCode();
        otpService.sendCode(email, OtpPurpose.EMAIL_VERIFICATION, code);
        pending.newCode(OtpService.hashCode(email, code), now.plus(otpService.props().ttl()), now);
    }

    /** The sign-up waiting for verification, unless it is older than {@code pending-registration-ttl}. */
    public Optional<PendingRegistration> findActive(String email) {
        Instant now = Instant.now();
        return pendingRepository.findByEmail(email).filter(p -> !isStale(p, now));
    }

    public boolean passwordMatches(PendingRegistration pending, String rawPassword) {
        return passwordEncoder.matches(rawPassword, pending.getPassword());
    }

    /** Sends a new code if the cooldown allows; never throws (used by login). */
    public void resendQuietly(String email) {
        try {
            resend(email);
        } catch (ApiException e) {
            // cooldown or email failure: the client can still call resend-otp
        }
    }

    private boolean isStale(PendingRegistration p, Instant now) {
        return p.getLastSentAt().plus(otpService.props().pendingRegistrationTtl()).isBefore(now);
    }

    private ApiException noPendingError(String email) {
        if (userRepository.existsByEmail(email)) {
            return new ConflictException("This email is already verified. Please log in");
        }
        return new ApiException(ErrorCode.NOT_FOUND,
                "No pending registration for this email (it may have expired). Please register again");
    }
}
