package com.example.expensetracker.auth;

import com.example.expensetracker.common.exception.ApiException;
import com.example.expensetracker.common.exception.BadRequestException;
import com.example.expensetracker.common.exception.ErrorCode;
import com.example.expensetracker.common.mail.MailService;
import com.example.expensetracker.common.util.HashUtils;
import com.example.expensetracker.user.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;

/**
 * Issues and checks one-time email codes. Callers own the transaction.
 * <ul>
 *   <li>Only SHA-256(userId:code) is stored.</li>
 *   <li>Issuing a new code invalidates the older open ones; a cooldown limits resends.</li>
 *   <li>Each wrong guess counts; after {@code max-attempts} the code is burned.</li>
 * </ul>
 */
@Service
public class OtpService {

    static final String NO_ACTIVE_CODE = "No active code for this email. Request a new code";
    static final String CODE_EXPIRED = "The code has expired. Request a new code";
    static final String TOO_MANY_ATTEMPTS = "Too many wrong attempts. This code is blocked, request a new code";
    private static final int RESET_TOKEN_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OtpCodeRepository otpCodeRepository;
    private final MailService mailService;
    private final OtpProperties props;

    public OtpService(OtpCodeRepository otpCodeRepository, MailService mailService, OtpProperties props) {
        this.otpCodeRepository = otpCodeRepository;
        this.mailService = mailService;
        this.props = props;
    }

    /** Creates a new code and emails it. 429 if the previous one was sent within the cooldown. */
    public void issue(User user, OtpPurpose purpose) {
        Instant now = Instant.now();
        otpCodeRepository.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), purpose)
                .ifPresent(last -> checkCooldown(last.getCreatedAt(), now));

        String code = generateCode();
        // Send first: if the email fails nothing is changed and the old code (if any) still works.
        mailService.send(user.getEmail(), subject(purpose), body(purpose, code));
        otpCodeRepository.invalidateOpen(user.getId(), purpose, now);
        otpCodeRepository.save(new OtpCode(user, purpose, hash(user, code), now.plus(props.ttl())));
    }

    /**
     * Checks the newest open code and marks it used. A wrong code counts an attempt, so the caller's
     * transaction must not roll back on {@link ApiException}.
     */
    public OtpCode verify(User user, OtpPurpose purpose, String code) {
        Instant now = Instant.now();
        OtpCode otp = otpCodeRepository
                .findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(user.getId(), purpose)
                .orElseThrow(() -> new BadRequestException(NO_ACTIVE_CODE));
        if (!otp.getExpiresAt().isAfter(now)) {
            otp.setUsedAt(now);
            throw new BadRequestException(CODE_EXPIRED);
        }
        if (!matches(otp.getCodeHash(), hash(user, code))) {
            otp.recordFailedAttempt();
            if (otp.getAttempts() >= props.maxAttempts()) {
                otp.setUsedAt(now);
                throw new ApiException(ErrorCode.TOO_MANY_REQUESTS, TOO_MANY_ATTEMPTS);
            }
            throw new BadRequestException(wrongCode(props.maxAttempts() - otp.getAttempts()));
        }
        otp.setUsedAt(now);
        return otp;
    }

    /** Attaches a short-lived reset token to a verified PASSWORD_RESET code. Returns the raw token. */
    public String issueResetToken(OtpCode otp) {
        String raw = HashUtils.randomToken(RESET_TOKEN_BYTES);
        otp.setResetToken(HashUtils.sha256Hex(raw), Instant.now().plus(props.resetTokenTtl()));
        return raw;
    }

    /** Validates and consumes a reset token (single use). Returns its user. */
    public User consumeResetToken(String rawToken) {
        OtpCode otp = otpCodeRepository.findByResetTokenHash(HashUtils.sha256Hex(rawToken))
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or already used reset token. Verify the reset code again"));
        if (!otp.getResetTokenExpiresAt().isAfter(Instant.now())) {
            otp.setResetToken(null, null);
            throw new BadRequestException("The reset token has expired. Verify the reset code again");
        }
        otp.setResetToken(null, null);
        return otp.getUser();
    }

    public long resetTokenTtlSeconds() {
        return props.resetTokenTtl().toSeconds();
    }

    // ---- shared helpers (also used by RegistrationService) ----

    static String wrongCode(int attemptsLeft) {
        return "Incorrect code. " + attemptsLeft + (attemptsLeft == 1 ? " attempt" : " attempts") + " left";
    }

    /** Random numeric code of the configured length. */
    String newCode() {
        return generateCode();
    }

    /** SHA-256(salt:code); salt is the user id or the pending email. */
    static String hashCode(String salt, String code) {
        return HashUtils.sha256Hex(salt + ":" + code.trim());
    }

    static boolean codeMatches(String storedHash, String salt, String code) {
        return matches(storedHash, hashCode(salt, code));
    }

    void sendCode(String email, OtpPurpose purpose, String code) {
        mailService.send(email, subject(purpose), body(purpose, code));
    }

    OtpProperties props() {
        return props;
    }

    /** 429 if {@code lastSentAt + cooldown} is still in the future. */
    void checkCooldown(Instant lastSentAt, Instant now) {
        Instant allowedAt = lastSentAt.plus(props.resendCooldown());
        if (allowedAt.isAfter(now)) {
            long seconds = Duration.between(now, allowedAt).toSeconds() + 1;
            throw new ApiException(ErrorCode.TOO_MANY_REQUESTS,
                    "Please wait " + seconds + " seconds before requesting a new code");
        }
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, props.length());
        return String.format("%0" + props.length() + "d", RANDOM.nextInt(bound));
    }

    private static String hash(User user, String code) {
        return hashCode(user.getId().toString(), code);
    }

    private static boolean matches(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    private static String subject(OtpPurpose purpose) {
        return purpose == OtpPurpose.EMAIL_VERIFICATION
                ? "Verify your Expense Tracker account"
                : "Reset your Expense Tracker password";
    }

    private String body(OtpPurpose purpose, String code) {
        String action = purpose == OtpPurpose.EMAIL_VERIFICATION ? "verify your email" : "reset your password";
        return """
                Your code to %s is: %s

                It expires in %d minutes. If you did not request this, you can ignore this email.
                """.formatted(action, code, props.ttl().toMinutes());
    }
}
