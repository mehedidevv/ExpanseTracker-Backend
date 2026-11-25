package com.example.expensetracker.auth;

import com.example.expensetracker.common.entity.BaseEntity;
import com.example.expensetracker.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** A one-time code sent by email. Only its hash is stored. */
@Entity
@Table(name = "otp_codes")
public class OtpCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Set when the code was verified, replaced by a newer code, or burned after too many attempts. */
    @Column(name = "used_at")
    private Instant usedAt;

    /** PASSWORD_RESET only: hash of the token returned by verify-reset-otp. Cleared once used. */
    @Column(name = "reset_token_hash", length = 64, unique = true)
    private String resetTokenHash;

    @Column(name = "reset_token_expires_at")
    private Instant resetTokenExpiresAt;

    /** Required by JPA. */
    protected OtpCode() {
    }

    public OtpCode(User user, OtpPurpose purpose, String codeHash, Instant expiresAt) {
        this.user = user;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
    }

    public void recordFailedAttempt() {
        attempts++;
    }

    // ---- getters / setters ----

    public User getUser() {
        return user;
    }

    public OtpPurpose getPurpose() {
        return purpose;
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

    public Instant getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
    }

    public String getResetTokenHash() {
        return resetTokenHash;
    }

    public Instant getResetTokenExpiresAt() {
        return resetTokenExpiresAt;
    }

    public void setResetToken(String resetTokenHash, Instant resetTokenExpiresAt) {
        this.resetTokenHash = resetTokenHash;
        this.resetTokenExpiresAt = resetTokenExpiresAt;
    }
}
