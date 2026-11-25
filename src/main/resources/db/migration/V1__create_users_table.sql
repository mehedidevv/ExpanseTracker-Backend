CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    password      VARCHAR(255) NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    currency_code CHAR(3)      NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_email_lower CHECK (email = lower(email))
);
