# Expense Tracker Backend

REST API for a personal expense tracker, consumed by a Flutter app.
Users record income and expenses, organise them by category, set monthly budgets and view reports.

**Stack:** Java 21 · Spring Boot 4.2 · PostgreSQL 15+ · Flyway · Spring Security (JWT) · springdoc · Testcontainers

## Quick start (local PostgreSQL)

```bash
./scripts/setup-local-db.sh                 # role "expense" + databases expense_tracker / expense_tracker_test
cp .env.example .env                        # then set JWT_SECRET=$(openssl rand -base64 48)
./scripts/dev.sh                            # dev mode: auto-restart on change (like npm run dev)
```

Using Docker instead? `docker compose up -d` replaces the first step.

| | |
|---|---|
| API base | http://localhost:8080/api/v1 |
| Swagger UI (dev) | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| Postman | import `postman/*.json`, run **Auth → Register** first |

## Features (PRD MVP)

| Area | Endpoints |
|---|---|
| Auth | register with email OTP, verify / resend OTP, login, forgot + reset password (OTP), refresh (rotation + re-use detection), logout |
| Profile | get / update profile, change password |
| Categories | 13 seeded defaults (read-only) + custom categories |
| Transactions | CRUD, filters (type, category, date range, amount range, note search), pagination, sorting |
| Budgets | overall or per-category monthly budgets with computed `spent` / `remaining` |
| Reports | summary, expense breakdown by category, 12-month trend |

## Tests

```bash
./mvnw clean verify
```

42 tests: service unit tests and API integration tests on a real PostgreSQL. Every feature has a
test showing that one user cannot read or change another user's data. Testcontainers is used when
Docker is available (CI); otherwise the local `expense_tracker_test` database.

## Documentation

| Doc | Contents |
|---|---|
| [docs/PROJECT_GUIDE.md](docs/PROJECT_GUIDE.md) | **Start here.** Every folder/file explained, DB connection, request flow, how to change things |
| [docs/DEVELOPMENT.md](docs/DEVELOPMENT.md) | **Run/stop the server, what to do after each change**, Node.js ↔ Maven commands |
| [docs/SETUP.md](docs/SETUP.md) | prerequisites, DB setup, env vars, run, test, Docker, troubleshooting |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | packages, layers, request flow, auth design, isolation, error model |
| [docs/DATABASE.md](docs/DATABASE.md) | ER diagram, migrations, constraints, schema-change rules |
| [docs/API.md](docs/API.md) | every endpoint with request/response examples and rules |
| [.agent/prd.md](.agent/prd.md) | product requirements |
| [.agent/rules.md](.agent/rules.md) | build order, engineering rules, decisions on open questions |
| [.agent/project.md](.agent/project.md) | folder structure |

## Project layout

```
src/main/java/com/example/expensetracker/
  common/       config · exception · response · security · entity · util
  auth/ user/ category/ transaction/ budget/ report/
src/main/resources/
  application.yml (+ -dev, -prod) · db/migration/V1..V8
src/test/java/...  support/ (AbstractIntegrationTest, TestApi) + one package per feature
docs/ · postman/ · scripts/ · Dockerfile · docker-compose.yml · .github/workflows/ci.yml
```
