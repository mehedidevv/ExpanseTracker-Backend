package com.example.expensetracker.auth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code app.otp.*} in application.yml.
 * {@code pendingRegistrationTtl}: an unverified sign-up is deleted this long after its last code was sent.
 */
@ConfigurationProperties(prefix = "app.otp")
public record OtpProperties(int length, Duration ttl, int maxAttempts, Duration resendCooldown,
                            Duration resetTokenTtl, Duration pendingRegistrationTtl) {
}
