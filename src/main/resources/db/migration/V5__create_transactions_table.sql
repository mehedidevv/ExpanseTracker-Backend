CREATE TABLE transactions (
    id               UUID          PRIMARY KEY,
    user_id          UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id      UUID          NOT NULL REFERENCES categories (id),
    type             VARCHAR(10)   NOT NULL,
    amount           NUMERIC(19,2) NOT NULL,
    note             VARCHAR(255),
    transaction_date DATE          NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_transactions_type CHECK (type IN ('EXPENSE', 'INCOME')),
    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_transactions_user_date     ON transactions (user_id, transaction_date DESC);
CREATE INDEX idx_transactions_user_category ON transactions (user_id, category_id);
