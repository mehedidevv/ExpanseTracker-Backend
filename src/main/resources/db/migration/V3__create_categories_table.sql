CREATE TABLE categories (
    id         UUID        PRIMARY KEY,
    user_id    UUID        REFERENCES users (id) ON DELETE CASCADE,  -- NULL = system default
    name       VARCHAR(50) NOT NULL,
    type       VARCHAR(10) NOT NULL,
    icon       VARCHAR(30),
    color      VARCHAR(30),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_categories_type CHECK (type IN ('EXPENSE', 'INCOME')),
    -- NULLS NOT DISTINCT (PostgreSQL 15+) so system defaults are unique too
    CONSTRAINT uk_categories_user_name_type UNIQUE NULLS NOT DISTINCT (user_id, name, type)
);
