CREATE TABLE budgets (
    id          UUID          PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    category_id UUID          REFERENCES categories (id),  -- NULL = overall budget
    amount      NUMERIC(19,2) NOT NULL,
    year        SMALLINT      NOT NULL,
    month       SMALLINT      NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_budgets_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_budgets_month CHECK (month BETWEEN 1 AND 12),
    -- NULLS NOT DISTINCT so only one overall budget per month
    CONSTRAINT uk_budgets_user_category_period UNIQUE NULLS NOT DISTINCT (user_id, category_id, year, month)
);
