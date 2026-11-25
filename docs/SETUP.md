# Local Setup Guide

Get the backend running on a fresh machine in about 5 minutes.

## 1. Prerequisites

| Tool | Version | Check |
|---|---|---|
| JDK | 21 | `java -version` |
| PostgreSQL | **15 or newer** (uses `UNIQUE NULLS NOT DISTINCT`) | `psql --version` |
| Docker (optional) | any | only needed for `docker compose` or Testcontainers |

Maven is not needed: use the bundled wrapper `./mvnw`.

## 2. Database

### Option A: PostgreSQL installed locally (Homebrew, Postgres.app, etc.)

```bash
./scripts/setup-local-db.sh
```

Creates (idempotent):

| Object | Value |
|---|---|
| role | `expense` / password `expense` |
| app database | `expense_tracker` |
| test database | `expense_tracker_test` |

Override with env vars: `PGHOST`, `PGPORT`, `PGSUPERUSER`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`.

### Option B: Docker

```bash
docker compose up -d      # postgres:17-alpine on localhost:5432, same credentials
```

Tables are **not** created by hand. Flyway runs `src/main/resources/db/migration/V*.sql`
automatically on the first app start.

## 3. Configuration (.env)

```bash
cp .env.example .env
openssl rand -base64 48   # paste the output into JWT_SECRET
```

Spring Boot loads `.env` automatically (`spring.config.import: optional:file:.env[.properties]`).
Real environment variables override `.env`.

| Variable | Default | Notes |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `dev` | `dev` = Swagger + SQL logs, `prod` = JSON logs, no Swagger |
| `PORT` | `8080` | |
| `DB_URL` | `jdbc:postgresql://localhost:5432/expense_tracker` | |
| `DB_USERNAME` / `DB_PASSWORD` | `expense` / `expense` | |
| `JWT_SECRET` | – | **Required**, ≥ 32 bytes. App refuses to start without it. |
| `JWT_ACCESS_TTL` | `15m` | access token lifetime |
| `JWT_REFRESH_TTL` | `7d` | refresh token lifetime |
| `CORS_ALLOWED_ORIGINS` | `*` | comma-separated origin patterns |
| `TEST_DB_URL` | `jdbc:postgresql://localhost:5432/expense_tracker_test` | tests, when Docker is absent |
| `MAIL_ENABLED` | `false` | `true` = send OTP emails over SMTP. `false` = print them in the server log (dev only) |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` | SMTP server (STARTTLS) |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | – | Gmail address + **App Password** (16 chars, not your normal password) |
| `MAIL_FROM` | `MAIL_USERNAME` | sender address shown in the email |
| `OTP_TTL` / `OTP_RESEND_COOLDOWN` | `10m` / `60s` | code lifetime / wait time between codes |
| `PENDING_REGISTRATION_TTL` | `1h` | unverified sign-ups are deleted this long after the last code (checked every 5 min) |
| `MAIL_VERIFY_DOMAIN` | `true` | reject sign-up emails whose domain has no mail server (DNS check). If DNS is unreachable the email is accepted. |

**Gmail App Password:** turn on 2-Step Verification, then Google Account → Security → *App passwords*
→ create one → put the 16 characters (no spaces) in `MAIL_PASSWORD`.
Without email set up, use `MAIL_ENABLED=false` and read the code from the server log
(line starting `MAIL DISABLED`).

## 4. Run

```bash
./scripts/dev.sh            # dev mode with auto-restart (recommended)
# or
./mvnw spring-boot:run      # plain start
```

Full guide to running, stopping and what to do after changes: [DEVELOPMENT.md](DEVELOPMENT.md).

| URL | What |
|---|---|
| http://localhost:8080/api/v1 | API base |
| http://localhost:8080/swagger-ui.html | Swagger UI (dev profile only) |
| http://localhost:8080/v3/api-docs | OpenAPI JSON |
| http://localhost:8080/actuator/health | Health check |

## 5. Try it with Postman

1. Import `postman/expense-tracker.postman_collection.json`
   and `postman/expense-tracker.local.postman_environment.json`.
2. Select the **Expense Tracker - Local** environment.
3. Run **Auth → Register**. Tokens are stored automatically.
4. Run **Categories → List categories** to capture default category ids.
5. Use any other request, or run the whole collection with the Collection Runner
   (it is ordered as an end-to-end scenario, ~30 requests).

## 6. Test

```bash
./mvnw clean verify
```

Integration tests use a real PostgreSQL:
- **Docker running** → Testcontainers starts `postgres:17-alpine` (this is what CI does).
- **No Docker** → the local `expense_tracker_test` database (created by the setup script).

The test database is wiped (user data only) before every test, never the dev database.

## 7. Production image

```bash
docker build -t expense-tracker .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/expense_tracker \
  -e DB_USERNAME=expense -e DB_PASSWORD=expense \
  -e JWT_SECRET="$(openssl rand -base64 48)" \
  expense-tracker
```

The image runs with `SPRING_PROFILES_ACTIVE=prod` as a non-root user.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `app.jwt.secret (JWT_SECRET) must be at least 32 bytes` | Set `JWT_SECRET` in `.env`. |
| `Connection refused localhost:5432` | Start PostgreSQL (`brew services start postgresql@16`) or `docker compose up -d`. |
| `password authentication failed for user "expense"` | Re-run `./scripts/setup-local-db.sh` (resets the password). |
| `syntax error at or near "NULLS"` in V3 | PostgreSQL is older than 15. Upgrade. |
| `Schema-validation: missing column ...` | An entity changed without a migration. Add `V{n+1}__*.sql`. |
| `Validate failed: Migration checksum mismatch` | An applied migration was edited. Revert it and add a new one. |
| Port 8080 in use | `PORT=8081 ./mvnw spring-boot:run` |

## OTP email not arriving

When the server starts, look for the `Mail:` line in the log:

| Log line | Meaning / fix |
|---|---|
| `Mail: SMTP login OK (...)` | Mail is set up correctly. Check Spam/Promotions, and use the **newest** code. |
| `Mail: SMTP login FAILED (...)` | Wrong `MAIL_USERNAME` / `MAIL_PASSWORD`. Gmail needs a 16-character **App Password** (spaces are removed automatically), not your normal password. |
| `Mail: MAIL_ENABLED=false ...` | Emails are not sent; the code is printed in the log (`MAIL DISABLED ... code ... is: 123456`). Set `MAIL_ENABLED=true` in `.env`. |
| no `Mail:` line at all | You are running an **old server**. Stop it (Ctrl+C or `lsof -ti tcp:8080 \| xargs kill`) and start again (`./scripts/dev.sh`). After changing `.env` or `pom.xml` a restart is always needed. |
| `Mail sent to ...` but nothing in the inbox | Check Spam; with Gmail, `you+test@gmail.com` addresses land in your own inbox (handy for testing). |
| `503 Could not send email` from the API | SMTP failed for that request; nothing was saved, just try again. |
