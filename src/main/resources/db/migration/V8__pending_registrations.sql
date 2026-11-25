-- Sign-up data waiting for email verification. The real `users` row is created only after the
-- OTP is verified, so a wrong/fake email never produces an account.
CREATE TABLE pending_registrations (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,     -- BCrypt
    full_name     VARCHAR(100) NOT NULL,
    currency_code CHAR(3)      NOT NULL,
    code_hash     VARCHAR(64)  NOT NULL,     -- SHA-256(email:code)
    attempts      INT          NOT NULL DEFAULT 0,
    expires_at    TIMESTAMPTZ  NOT NULL,
    last_sent_at  TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_pending_registrations_email UNIQUE (email),
    CONSTRAINT ck_pending_registrations_email_lower CHECK (email = lower(email))
);

-- Unverified accounts from the previous flow are not real accounts: remove them (they can sign up again).
DELETE FROM users WHERE email_verified = FALSE;
