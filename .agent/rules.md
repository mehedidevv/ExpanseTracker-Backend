# Engineering Rules and Execution Order

This file tells any developer (or agent) **what to build in which order** and **which rules
must never be broken**. Source of truth for requirements: [`prd.md`](prd.md).
Source of truth for layout: [`project.md`](project.md).

---

## 1. Execution order (follow strictly, one milestone at a time)

Each step depends on the one before it. Do not start a step until the previous one
compiles and its tests pass (`./mvnw clean verify`).

| # | Milestone | Depends on | Delivers | Done when |
|---|---|---|---|---|
| 1 | Project setup | – | config profiles, Flyway V1–V6, `common/` (BaseEntity, errors, PageResponse), Docker, CI | app starts on a clean DB, schema validates |
| 2 | Auth + user profile | 1 | Spring Security, JWT access token, hashed refresh tokens (rotation), `/auth/**`, `/users/me/**` | register → login → refresh → logout works; protected routes return 401 without token |
| 3 | Categories | 2 (needs current user) | list defaults + own, CRUD custom, 403 on defaults, 409 when in use | category tests pass |
| 4 | Transactions | 3 (needs categories) | CRUD, filters, pagination, sort whitelist, type must match category | transaction tests pass |
| 5 | Budgets | 4 (spent comes from transactions) | CRUD, `spent` / `remaining` per month | budget tests pass |
| 6 | Reports | 4 | summary, by-category, monthly trend | report tests pass |
| 7 | Hardening and delivery | 1–6 | isolation tests, Postman collection, docs, local DB script | all acceptance criteria in PRD §10 met |

Why this order: every business record belongs to a user (needs auth), every transaction
needs a category, budgets and reports are computed from transactions.

---

## 2. Non-negotiable rules

### Data and money
- Money is `BigDecimal` in Java and `NUMERIC(19,2)` in SQL. **Never** `double`/`float`.
- Amounts must be `> 0` with at most 2 decimals (`@DecimalMin(exclusive)` + `@Digits`).
- `transaction_date` is a `LocalDate` (`DATE`). Audit columns are `Instant` (`TIMESTAMPTZ`, UTC).
- All ids are `UUID`.

### Schema
- The schema is changed **only** by Flyway migrations in `src/main/resources/db/migration`.
- Never edit a migration that has already run anywhere. Add `V{n+1}__description.sql` instead.
- Hibernate runs with `ddl-auto: validate`: an entity that does not match the schema fails startup.

### Security and isolation
- Every query on user data filters by the current user's id (`findByIdAndUserId`, specifications
  starting with `user.id = :userId`). A record owned by someone else is reported as **404**, not 403,
  so ids are not leaked.
- System default categories (`user_id IS NULL`) are read-only → **403**.
- Passwords: BCrypt. Refresh tokens: stored as SHA-256 hash, rotated on every refresh,
  re-use of a revoked token revokes all of that user's tokens.
- No secrets in code. Everything secret comes from env vars / `.env` (git-ignored).

### Code layout
- Feature-first packages (`auth`, `user`, `category`, `transaction`, `budget`, `report`).
  Shared code only in `common/`.
- Controller → Service → Repository. Controllers hold no business logic.
- Entities never leave the service layer; controllers return DTO records via a Mapper.
- Multi-step writes are `@Transactional` service methods. Reads use `@Transactional(readOnly = true)`.
- List endpoints are always paginated (default 20, max 100). Sort fields are whitelisted.
- Avoid N+1: use `@EntityGraph` / fetch joins when a list response touches a relation.

### API
- Prefix `/api/v1`. JSON `camelCase`. Dates ISO-8601.
- Status codes: 200 read/update/delete/logout/password-change, 201 create. Never an empty body.
- Every success body is `ApiResponse` (`success, status, message, data, timestamp, path`); every error is `ApiError` (`success: false, status, code, message, fieldErrors, timestamp, path`).
- Every error uses the `ApiError` shape from PRD §7. Never expose stack traces.

### Testing
- Integration tests extend `support/AbstractIntegrationTest` (real PostgreSQL: Testcontainers if
  Docker is available, otherwise the local `expense_tracker_test` database).
- Every feature has a test proving user A cannot read or change user B's data.

---

## 3. Decisions on PRD open questions (§11)

| Question | Decision for MVP |
|---|---|
| Soft vs hard delete for transactions | **Hard delete.** Simpler; can add `deleted_at` later via migration. |
| Budget carry-over | **No.** Each month stands alone. |
| Email verification / password reset | **Added (post-MVP):** 6-digit email OTP for sign-up verification and forgot-password. No `users` row exists until the sign-up OTP is verified (pending_registrations); sign-up emails must pass a format + DNS domain check. Codes hashed, 10 min, 5 attempts, 60 s resend cooldown. |
| Currencies at launch | Any 3-letter uppercase ISO 4217 code; stored per user, display only. |
| Duplicate category names | A user cannot create a category whose name (case-insensitive) matches a default or their own category of the same type. |
| Category type change | Not allowed while the category is used by transactions (409). |
