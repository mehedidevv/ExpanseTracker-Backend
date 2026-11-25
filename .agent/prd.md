# Expense Tracker Backend: Product Requirements Document (PRD)

> Status: Draft v1 (MVP)
> Stack: Java 21, Spring Boot, Maven, PostgreSQL, Flyway, Spring Security (JWT)
> Client: Flutter mobile app (consumes REST API)

---

## 1. Overview

A REST backend for a personal expense tracker. Users record income and expenses,
organise them by category, set monthly budgets, and view summary reports.

## 2. Goals

- Secure, multi-user REST API for the Flutter client.
- Accurate money handling (no floating-point errors).
- Clean, maintainable, feature-first codebase that can grow.
- Production-ready basics: validation, error handling, tests, Docker, CI.

## 3. Non-Goals (MVP)

- Multi-currency conversion / exchange rates (each user has ONE currency).
- Bank/SMS auto-import, OCR receipt scanning.
- Shared/family accounts, recurring transactions, file attachments.
- Push notifications, social login (may be added later).

## 4. Users and Roles

| Role | Description |
|---|---|
| USER | Normal registered user. Can only access their own data. |
| ADMIN | Reserved for future use (manage default categories). Not required in MVP. |

Every business record belongs to exactly one user. A user must NEVER be able to
read or modify another user's data (enforce in the service layer by user id).

---

## 5. Functional Requirements

### 5.1 Authentication
- FR-A1: Register with email, password, full name, currency code.
- FR-A2: Login with email and password, returns access token + refresh token.
- FR-A3: Refresh access token using a valid refresh token (token rotation).
- FR-A4: Logout revokes the refresh token.
- Rules:
  - Email is unique (case-insensitive).
  - Password min 8 chars, stored with BCrypt.
  - Access token ~15 min, refresh token ~7 days.
  - Refresh tokens are stored hashed in the database so they can be revoked.

### 5.2 User Profile
- FR-U1: Get current user profile.
- FR-U2: Update full name and currency.
- FR-U3: Change password (requires current password).

### 5.3 Categories
- FR-C1: List categories (system defaults + the user's own).
- FR-C2: Create, update, delete a custom category.
- Rules:
  - Type is `EXPENSE` or `INCOME`.
  - System default categories (user_id = NULL) are read-only for users.
  - Name is unique per user per type.
  - A category that has transactions cannot be deleted (return 409 Conflict).

### 5.4 Transactions
- FR-T1: Create a transaction (type, amount, category, date, optional note).
- FR-T2: Get one transaction.
- FR-T3: List transactions with pagination, sorting and filters:
  type, category, date range, min/max amount, text search on note.
- FR-T4: Update and delete a transaction.
- Rules:
  - Amount must be greater than 0, max 2 decimal places.
  - Transaction type must match the category type.
  - `transaction_date` is a DATE (the day the money moved), not a timestamp.
  - Default sort: `transaction_date DESC, created_at DESC`.

### 5.5 Budgets
- FR-B1: Create a monthly budget (overall, or for one expense category).
- FR-B2: List budgets for a given month with `spent` and `remaining` computed.
- FR-B3: Update and delete a budget.
- Rules:
  - One budget per (user, category, year, month). Overall budget = category is NULL.
  - Budget amount must be greater than 0.
  - `spent` is calculated from expense transactions in that month.

### 5.6 Reports
- FR-R1: Summary for a date range: total income, total expense, balance.
- FR-R2: Expense breakdown by category for a date range (amount + percentage).
- FR-R3: Monthly trend for a given year (income and expense per month).

---

## 6. Data Model (PostgreSQL)

All ids are `UUID`. All timestamps are `TIMESTAMPTZ` (stored in UTC).
Money columns are `NUMERIC(19,2)`.

### users
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| email | VARCHAR(255) | unique, stored lowercase |
| password | VARCHAR(255) | BCrypt hash |
| full_name | VARCHAR(100) | |
| currency_code | CHAR(3) | ISO 4217, e.g. BDT |
| created_at, updated_at | TIMESTAMPTZ | |

### refresh_tokens
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK users | on delete cascade |
| token_hash | VARCHAR(255) | unique |
| expires_at | TIMESTAMPTZ | |
| revoked | BOOLEAN | default false |
| created_at | TIMESTAMPTZ | |

### categories
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK users NULL | NULL = system default |
| name | VARCHAR(50) | |
| type | VARCHAR(10) | EXPENSE / INCOME |
| icon, color | VARCHAR(30) NULL | optional UI hints |
| created_at, updated_at | TIMESTAMPTZ | |
| | | unique (user_id, name, type) |

### transactions
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK users | indexed |
| category_id | UUID FK categories | |
| type | VARCHAR(10) | EXPENSE / INCOME |
| amount | NUMERIC(19,2) | check > 0 |
| note | VARCHAR(255) NULL | |
| transaction_date | DATE | indexed with user_id |
| created_at, updated_at | TIMESTAMPTZ | |

Indexes: `(user_id, transaction_date DESC)`, `(user_id, category_id)`.

### budgets
| Column | Type | Notes |
|---|---|---|
| id | UUID PK | |
| user_id | UUID FK users | |
| category_id | UUID FK categories NULL | NULL = overall budget |
| amount | NUMERIC(19,2) | check > 0 |
| year | SMALLINT | |
| month | SMALLINT | 1 to 12 |
| created_at, updated_at | TIMESTAMPTZ | |
| | | unique (user_id, category_id, year, month) |

---

## 7. REST API (prefix: `/api/v1`)

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | /auth/register | No | Register |
| POST | /auth/login | No | Login |
| POST | /auth/refresh | No | Refresh tokens |
| POST | /auth/logout | Yes | Revoke refresh token |
| GET | /users/me | Yes | Current profile |
| PUT | /users/me | Yes | Update profile |
| PUT | /users/me/password | Yes | Change password |
| GET | /categories | Yes | List (filter by type) |
| POST | /categories | Yes | Create |
| PUT | /categories/{id} | Yes | Update |
| DELETE | /categories/{id} | Yes | Delete |
| GET | /transactions | Yes | List (paged, filtered) |
| POST | /transactions | Yes | Create |
| GET | /transactions/{id} | Yes | Get one |
| PUT | /transactions/{id} | Yes | Update |
| DELETE | /transactions/{id} | Yes | Delete |
| GET | /budgets?year=&month= | Yes | List with spent/remaining |
| POST | /budgets | Yes | Create |
| PUT | /budgets/{id} | Yes | Update |
| DELETE | /budgets/{id} | Yes | Delete |
| GET | /reports/summary?from=&to= | Yes | Income/expense/balance |
| GET | /reports/by-category?from=&to= | Yes | Expense breakdown |
| GET | /reports/monthly?year= | Yes | Monthly trend |

### Conventions
- JSON field names: `camelCase`. Dates: ISO-8601 (`2026-11-25`). Money as number with 2 decimals.
- Pagination query params: `page` (0-based), `size` (default 20, max 100), `sort`.
- Paged response: `{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0 }`.
- Success status codes: 200, 201 (create), 204 (delete).

### Error response (all errors)
```json
{
  "timestamp": "2026-11-25T10:00:00Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/transactions",
  "fieldErrors": [{ "field": "amount", "message": "must be greater than 0" }]
}
```
Common codes: `VALIDATION_ERROR` (400), `UNAUTHORIZED` (401), `FORBIDDEN` (403),
`NOT_FOUND` (404), `CONFLICT` (409), `INTERNAL_ERROR` (500).
Never leak stack traces or internal details in responses.

---

## 8. Non-Functional Requirements

- Security: BCrypt, JWT, per-user data isolation, input validation, no secrets in code.
- Performance: list endpoints always paginated; avoid N+1 queries; indexes as above.
- Reliability: all multi-step writes in `@Transactional` service methods.
- Observability: structured logs, Actuator health endpoint.
- Testing: unit tests for services, integration tests for repositories and APIs
  using Testcontainers (real PostgreSQL).
- API docs: OpenAPI/Swagger UI available in dev profile only.
- Deployability: Docker image, GitHub Actions CI (build + test on every push).

---

## 9. Milestones

1. Project setup (config, Docker Postgres, Flyway, base structure, error handling).
2. Auth and user profile (register, login, refresh, logout, JWT filter).
3. Categories (with seeded default categories via Flyway).
4. Transactions (CRUD, filters, pagination).
5. Budgets.
6. Reports.
7. Tests, Docker image, CI pipeline, production config.

## 10. Acceptance Criteria (MVP done when)

- All endpoints in section 7 work and are documented in Swagger.
- A user cannot access another user's data (verified by tests).
- Money values never lose precision.
- `./mvnw clean verify` passes, including integration tests.
- App starts from a clean database using only Flyway migrations.

## 11. Open Questions

- Soft delete vs hard delete for transactions?
- Should budgets support carry-over of unused amount?
- Email verification and password reset: needed in MVP?
- Which currencies must be supported at launch?