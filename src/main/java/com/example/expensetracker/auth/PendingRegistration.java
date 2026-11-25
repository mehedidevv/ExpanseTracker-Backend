package com.example.expensetracker.auth;

import com.example.expensetracker.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Sign-up waiting for its email OTP. Becomes a {@code User} only after verification. */
@Entity
@Table(name = "pending_registrations")
public class PendingRegistration extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash. */
    @Column(nullable = false)
    private String password;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "currency_code", nullable = false, length = 3)
    private String currencyCode;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;

    /** Required by JPA. */
    protected PendingRegistration() {
    }

    public PendingRegistration(String email) {
        this.email = email;
    }

    /** Replaces the details and the code (new sign-up attempt or resend). Resets the attempt counter. */
    public void newCode(String codeHash, Instant expiresAt, Instant sentAt) {
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.lastSentAt = sentAt;
        this.attempts = 0;
    }

    public void recordFailedAttempt() {
        attempts++;
    }

    // ---- getters / setters ----

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public int getAttempts() {
        return attempts;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastSentAt() {
        return lastSentAt;
    }
}
