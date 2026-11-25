package com.example.expensetracker.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Housekeeping: daily (03:00) removes expired refresh tokens and OTP codes; every 5 minutes removes
 * unverified sign-ups older than the pending-registration TTL.
 */
@Component
public class AuthCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AuthCleanupJob.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final PendingRegistrationRepository pendingRepository;
    private final OtpProperties otpProperties;

    public AuthCleanupJob(RefreshTokenRepository refreshTokenRepository, OtpCodeRepository otpCodeRepository,
                          PendingRegistrationRepository pendingRepository, OtpProperties otpProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.pendingRepository = pendingRepository;
        this.otpProperties = otpProperties;
    }

    /**
     * Every 5 minutes: deletes sign-ups that were never verified within {@code app.otp.pending-registration-ttl}
     * (default 1 hour), so the email can be registered again from scratch.
     */
    @Transactional
    @Scheduled(fixedDelay = 5, timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public int purgeUnverifiedSignUps() {
        int deleted = pendingRepository.deleteLastSentBefore(
                Instant.now().minus(otpProperties.pendingRegistrationTtl()));
        if (deleted > 0) {
            log.info("Removed {} unverified sign-ups older than {}", deleted, otpProperties.pendingRegistrationTtl());
        }
        return deleted;
    }

    @Transactional
    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpired() {
        Instant cutoff = Instant.now().minus(1, ChronoUnit.DAYS);
        int tokens = refreshTokenRepository.deleteExpiredBefore(cutoff);
        int codes = otpCodeRepository.deleteExpiredBefore(cutoff);
        if (tokens + codes > 0) {
            log.info("Purged {} expired refresh tokens and {} expired OTP codes", tokens, codes);
        }
    }
}
