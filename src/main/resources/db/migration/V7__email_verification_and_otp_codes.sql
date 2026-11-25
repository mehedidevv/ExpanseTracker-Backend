-- Email verification flag. Accounts created before this migration are treated as verified.
ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE users SET email_verified = TRUE;

-- One-time codes sent by email (registration + password reset).
-- The code itself is never stored, only SHA-256(user_id:code).
-- After a PASSWORD_RESET code is verified, a short-lived reset token (hashed) is attached to the row.
CREATE TABLE otp_codes (
    id                     UUID         PRIMARY KEY,
    user_id                UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    purpose                VARCHAR(30)  NOT NULL,
    code_hash              VARCHAR(64)  NOT NULL,
    attempts               INT          NOT NULL DEFAULT 0,
    expires_at             TIMESTAMPTZ  NOT NULL,
    used_at                TIMESTAMPTZ,
    reset_token_hash       VARCHAR(64),
    reset_token_expires_at TIMESTAMPTZ,
    created_at             TIMESTAMPTZ  NOT NULL,
    updated_at             TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_otp_codes_purpose CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    CONSTRAINT uk_otp_codes_reset_token_hash UNIQUE (reset_token_hash)
);

CREATE INDEX idx_otp_codes_user_purpose ON otp_codes (user_id, purpose, created_at DESC);
