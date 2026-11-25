# Project Guide: Where Everything Lives and How It Works

> **Who this is for:** someone new to this project, or new to Spring Boot.
> Read this one file and you will know what every folder and file does, how a request travels
> through the code, how the database is connected, and **exactly where to make changes** later.

---

## Table of contents

1. [The big picture in 1 minute](#1-the-big-picture-in-1-minute)
2. [Words you need to know (glossary)](#2-words-you-need-to-know-glossary)
3. [The full folder map](#3-the-full-folder-map)
4. [Database connection: from `.env` to PostgreSQL](#4-database-connection-from-env-to-postgresql)
5. [What happens when the app starts](#5-what-happens-when-the-app-starts)
6. [Root files explained](#6-root-files-explained)
7. [`src/main/resources`: configuration and SQL](#7-srcmainresources-configuration-and-sql)
8. [`src/main/java`: the code, package by package](#8-srcmainjava-the-code-package-by-package)
9. [Journey of one request (file by file)](#9-journey-of-one-request-file-by-file)
10. [`src/test`: the tests](#10-srctest-the-tests)
11. [Other folders: docs, postman, scripts, .github, .agent](#11-other-folders)
12. [How to change things (recipes)](#12-how-to-change-things-recipes)
13. [Daily commands cheat sheet](#13-daily-commands-cheat-sheet)
14. [Rules: what NOT to do](#14-rules-what-not-to-do)

---

## 1. The big picture in 1 minute

This is a **backend** (server) for an expense tracker app. The Flutter mobile app sends HTTP requests
(e.g. "create an expense of 250 taka"), this server checks them, saves data in a **PostgreSQL**
database, and sends back JSON.

```
 Flutter app  ──HTTP/JSON──►  Spring Boot server (this project)  ──SQL──►  PostgreSQL database
              ◄─────────────                                      ◄──────
```

Inside the server, every request goes through the same layers:

```
Request
  │
  ▼
JwtAuthFilter      "Who is calling?"           (common/security)
  │
  ▼
Controller         "Which URL? Is the input valid?"   (xxxController.java)
  │
  ▼
Service            "Business rules"             (xxxService.java)
  │
  ▼
Repository         "Talk to the database"       (xxxRepository.java)
  │
  ▼
PostgreSQL
```

The answer goes back up the same way. Along the way, a **Mapper** turns database objects (Entities)
into response objects (DTOs).

**Remember this one sentence:**
> URL handling lives in the **Controller**, rules in the **Service**, SQL in the **Repository**,
> table shape in the **Entity** + **Flyway migration**, request/response JSON shape in **dto/**.

---

## 2. Words you need to know (glossary)

| Word | Meaning in this project | Example file |
|---|---|---|
| **Spring Boot** | The framework. It starts the server, creates objects for you and wires them together. | `ExpenseTrackerApplication.java` |
| **Annotation** | A tag starting with `@` that tells Spring what a class/method is. | `@RestController`, `@Service` |
| **Bean** | An object Spring creates and manages. Classes marked `@Service`, `@Component`, `@Repository`, `@RestController`, `@Configuration` + `@Bean` methods become beans. | `JwtService` |
| **Dependency injection** | You list what you need as `private final` fields + a constructor that sets them; Spring calls that constructor and passes the beans in. | `AuthService(UserRepository userRepository, ...)` |
| **Entity** | A Java class that represents **one row of a table**. | `Transaction.java` ↔ `transactions` table |
| **Repository** | Interface for database queries. Spring writes the SQL from the method name. | `findByIdAndUserId(...)` |
| **Service** | Class holding business logic (rules). | `TransactionService.java` |
| **Controller** | Class that maps a URL to a Java method. | `@GetMapping("/{id}")` |
| **DTO** | "Data Transfer Object": the shape of JSON coming in (Request) or going out (Response). Java `record`s. | `CreateTransactionRequest` |
| **Mapper** | Converts Entity → Response DTO. | `TransactionMapper.java` |
| **Flyway migration** | A numbered `.sql` file that creates/changes tables. Runs automatically once. | `V5__create_transactions_table.sql` |
| **JPA / Hibernate** | The library that turns Entities into SQL. | – |
| **JWT** | The signed "access token" string the app sends in `Authorization: Bearer ...`. | `JwtService.java` |
| **Profile** | A set of config for an environment: `dev`, `prod`, `test`. | `application-dev.yml` |
| **Getters / setters** | Plain Java methods at the bottom of each Entity (`getId()`, `setAmount()`). The project does **not** use Lombok, so every method is visible and works in any IDE. | `Transaction.java` |
| **`@Transactional`** | "Do all DB work in this method as one unit; if anything fails, undo everything." | Service methods |

---

## 3. The full folder map

```
expanse_tracker/
│
├── .agent/                         ← Project planning (read first)
│   ├── prd.md                        Product requirements: WHAT to build
│   ├── project.md                    Folder structure plan
│   └── rules.md                      Build order + coding rules + decisions
│
├── .github/workflows/ci.yml        ← GitHub runs build + tests on every push
├── .mvn/                           ← Maven wrapper internals (don't touch)
│
├── docs/                           ← Documentation (you are here)
│   ├── PROJECT_GUIDE.md              This file
│   ├── DEVELOPMENT.md                Run server, dev mode, what to do after changes
│   ├── SETUP.md                      Install + run + troubleshooting
│   ├── ARCHITECTURE.md               Design decisions in depth
│   ├── DATABASE.md                   Tables, ER diagram, constraints
│   └── API.md                        Every endpoint with examples
│
├── postman/                        ← Import into Postman to test the API
│   ├── expense-tracker.postman_collection.json
│   └── expense-tracker.local.postman_environment.json
│
├── scripts/
│   ├── setup-local-db.sh           ← Creates DB user + databases on your PC
│   └── dev.sh                      ← Dev mode: run server + auto-restart on change
│
├── src/
│   ├── main/
│   │   ├── java/com/example/expensetracker/    ← ALL application code
│   │   │   ├── ExpenseTrackerApplication.java    Starting point (main method)
│   │   │   ├── common/                           Shared code used by all features
│   │   │   │   ├── config/                         App-wide settings (CORS, Swagger, ...)
│   │   │   │   ├── entity/                         BaseEntity (id + timestamps)
│   │   │   │   ├── exception/                      Error classes + global error handler
│   │   │   │   ├── response/                       ApiResponse, ApiError, PageResponse
│   │   │   │   ├── security/                       Login/JWT/Spring Security setup
│   │   │   │   ├── mail/                           Sending emails (OTP codes)
│   │   │   │   └── util/                           Small helpers (hashing)
│   │   │   ├── auth/                             Register + OTP, login, forgot/reset password, refresh, logout
│   │   │   ├── user/                             Profile, change password
│   │   │   ├── category/                         Categories
│   │   │   ├── transaction/                      Income/expense records
│   │   │   ├── budget/                           Monthly budgets
│   │   │   └── report/                           Summary/charts data
│   │   │
│   │   └── resources/                          ← Config + SQL (not Java)
│   │       ├── application.yml                   Main config (DB, JWT, ...)
│   │       ├── application-dev.yml               Extra config for local dev
│   │       ├── application-prod.yml              Extra config for production
│   │       └── db/migration/                     Flyway SQL files V1..V8
│   │
│   └── test/
│       ├── java/com/example/expensetracker/    ← Tests (same packages as main)
│       └── resources/application-test.yml      Config used only by tests
│
├── .env                ← YOUR secrets (DB password, JWT secret). Git-ignored. Never commit.
├── .env.example        ← Template of .env (safe to commit)
├── .gitignore          ← Files git must ignore
├── docker-compose.yml  ← Start PostgreSQL in Docker (optional)
├── Dockerfile          ← Build a production Docker image
├── mvnw / mvnw.cmd     ← Maven wrapper: use ./mvnw instead of installing Maven
├── pom.xml             ← Dependencies (libraries) + build settings
└── README.md           ← Front page of the project
```

### Inside every feature folder (same pattern everywhere)

Take `transaction/` as the example. **Every feature looks like this**, so once you understand one,
you understand all of them:

```
transaction/
├── Transaction.java                 ENTITY      one row of the "transactions" table
├── TransactionRepository.java       REPOSITORY  database queries
├── TransactionService.java          SERVICE     business rules
├── TransactionController.java       CONTROLLER  URLs (/api/v1/transactions)
├── TransactionMapper.java           MAPPER      Entity → Response DTO
├── TransactionSpecifications.java   (extra)     dynamic search filters
└── dto/                             DTOs        JSON shapes
    ├── CreateTransactionRequest.java   JSON body for POST
    ├── UpdateTransactionRequest.java   JSON body for PUT
    ├── TransactionFilter.java          query params for GET list
    └── TransactionResponse.java        JSON returned to the app
```

---

## 4. Database connection: from `.env` to PostgreSQL

This is the full chain. If the DB does not connect, check each step in order.

```
.env  ──►  application.yml  ──►  Spring DataSource (HikariCP pool)  ──►  PostgreSQL
                                        │
                                        ├──► Flyway   (creates/updates tables at startup)
                                        └──► Hibernate/JPA (Entities ↔ rows, used by Repositories)
```

### Step 1: `.env` (project root) holds the real values

```properties
DB_URL=jdbc:postgresql://localhost:5432/expense_tracker
DB_USERNAME=expense
DB_PASSWORD=expense
JWT_SECRET=...long random string...
```

- `localhost:5432` = PostgreSQL running on your PC, default port.
- `expense_tracker` = database name.
- This file is **git-ignored**, so passwords never go to GitHub.

### Step 2: `src/main/resources/application.yml` reads them

```yaml
spring:
  config:
    import: optional:file:.env[.properties]     # ← loads .env
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/expense_tracker}
    username: ${DB_USERNAME:expense}
    password: ${DB_PASSWORD:expense}
```

`${DB_URL:default}` means "use the env value `DB_URL`, or the default after `:` if missing".
Priority: **real environment variable > `.env` > default in yml**.

### Step 3: Spring Boot creates the connection automatically

Because `pom.xml` contains `spring-boot-starter-data-jpa` and the `postgresql` driver, Spring Boot
builds a connection pool (HikariCP) from those three values. **There is no Java code you write for
the connection.**

### Step 4: Flyway creates the tables

`spring-boot-starter-flyway` (in `pom.xml`) runs every file in `src/main/resources/db/migration`
that hasn't run yet, in version order, and records it in the table `flyway_schema_history`.

### Step 5: Hibernate checks the tables match the Entities

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

At startup Hibernate compares each `@Entity` class with the real table. If a column is missing or
the type is wrong, **the app refuses to start** (this protects you from silent bugs).

### Step 6: Repositories use the connection

When `TransactionService` calls `transactionRepository.save(tx)`, Spring Data JPA generates the
`INSERT` and runs it through the pool.

### Where to change things

| I want to... | Change |
|---|---|
| use another DB name / password | `.env` (and re-run `scripts/setup-local-db.sh` with the new values) |
| connect to a remote/cloud DB | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` in `.env` or server env vars |
| create the local DB from scratch | `./scripts/setup-local-db.sh` |
| see SQL in the console | already on in dev: `logging.level.org.hibernate.SQL: debug` in `application-dev.yml` |
| look at the data | `psql -h localhost -U expense -d expense_tracker` (password `expense`), or any GUI (DBeaver, TablePlus, pgAdmin) with the same values |

---

## 5. What happens when the app starts

`./mvnw spring-boot:run` →

1. `ExpenseTrackerApplication.main()` runs.
2. Spring reads `application.yml` + `.env` + `application-dev.yml` (profile `dev` is the default).
3. Spring scans every package under `com.example.expensetracker` and creates all beans
   (controllers, services, repositories, configs).
4. `JwtService` checks `JWT_SECRET` is at least 32 bytes, **or startup stops** with a clear message.
5. DB connection pool opens → **Flyway** applies new migrations → **Hibernate** validates the schema.
6. `SecurityConfig` installs the security rules + `JwtAuthFilter`.
7. Tomcat (embedded web server) listens on port `8080`.
8. Log line: `Started ExpenseTrackerApplication in X seconds`.

---

## 6. Root files explained

| File | What it is | When you edit it |
|---|---|---|
| `pom.xml` | Maven project file: Java version, Spring Boot version, list of libraries (dependencies), build plugins. | Adding a library (e.g. email sender). |
| `mvnw`, `mvnw.cmd`, `.mvn/` | Maven wrapper. Downloads the right Maven automatically. | Never. |
| `.env` | Your local secrets and settings. Git-ignored. | Changing DB password, JWT secret, port. |
| `.env.example` | Template showing every variable `.env` can have. | When you add a new env variable (document it here). |
| `docker-compose.yml` | Runs PostgreSQL 17 in Docker on port 5432 with the same credentials. | Only if you use Docker. |
| `Dockerfile` | Builds a small production image: compiles with JDK 21, runs on JRE 21 as a non-root user, profile `prod`. | Changing how production runs. |
| `.github/workflows/ci.yml` | GitHub Actions: on push/PR runs `./mvnw clean verify`. | Adding CI steps (deploy, lint). |
| `.gitignore` | Files git ignores (`target/`, `.idea`, `.env`). | Rarely. |
| `README.md` | Project front page. | When big things change. |
| `HELP.md` | Auto-generated by Spring Initializr. Not used. Safe to delete. | – |

---

## 7. `src/main/resources`: configuration and SQL

### `application.yml` (always loaded)

| Section | What it controls |
|---|---|
| `spring.config.import` | Loads `.env`. |
| `spring.profiles.active` | Which extra profile file to load (`dev` default). |
| `spring.datasource.*` | Database URL/user/password (see section 4). |
| `spring.jpa.open-in-view: false` | DB session closes after the service finishes (prevents hidden slow queries). |
| `spring.jpa.hibernate.ddl-auto: validate` | Hibernate never changes tables; only checks them. |
| `hibernate.jdbc.time_zone: UTC` | All timestamps saved in UTC. |
| `spring.flyway.*` | Migrations on, location `db/migration`. |
| `spring.data.web.pageable.*` | Paging defaults: size 20, max 100. |
| `server.port` | `8080` (or `PORT` env). |
| `server.error.include-stacktrace: never` | Never show Java errors to clients. |
| `management.*` | Actuator: exposes `/actuator/health` and `/actuator/info`. |
| `springdoc.*` | Swagger **off** by default (turned on in dev). |
| `app.cors.allowed-origins` | Which web origins may call the API. |
| `app.jwt.*` | JWT secret, issuer, access-token lifetime (15m), refresh-token lifetime (7d). |

### `application-dev.yml` (only when profile = `dev`)
Swagger UI **on**, SQL logging on, debug logs for our package.

### `application-prod.yml` (only when profile = `prod`)
Swagger **off**, JSON (ECS) structured logs for log servers.

### `db/migration/` (Flyway SQL)

| File | Creates |
|---|---|
| `V1__create_users_table.sql` | `users` |
| `V2__create_refresh_tokens_table.sql` | `refresh_tokens` |
| `V3__create_categories_table.sql` | `categories` |
| `V4__seed_default_categories.sql` | inserts 13 default categories (Food, Rent, Salary, ...) |
| `V5__create_transactions_table.sql` | `transactions` + indexes |
| `V6__create_budgets_table.sql` | `budgets` |
| `V7__email_verification_and_otp_codes.sql` | `users.email_verified` column + `otp_codes` |
| `V8__pending_registrations.sql` | `pending_registrations` (sign-ups waiting for OTP) |

Naming rule: `V<number>__<description>.sql` (two underscores). **Never edit a file that already ran**;
add a new one (`V7__...`). See recipe 12.2.

---

## 8. `src/main/java`: the code, package by package

Root package: `com.example.expensetracker`.

### 8.1 `ExpenseTrackerApplication.java`
The `main` method. `@SpringBootApplication` = "scan this package and everything below it".
`@ConfigurationPropertiesScan` = "find classes like `JwtProperties` that read config".
**Must stay in the root package**, otherwise Spring won't find the feature packages.

---

### 8.2 `common/` (shared by every feature)

#### `common/config/`: app-wide settings

| File | Does |
|---|---|
| `CorsConfig.java` | Lets browsers/web apps from allowed origins call `/api/**`. Origins from `app.cors.allowed-origins`. |
| `OpenApiConfig.java` | Swagger title + adds the "Authorize (Bearer token)" button. |
| `JpaAuditingConfig.java` | Turns on automatic `createdAt` / `updatedAt` filling. |
| `SchedulingConfig.java` | Turns on scheduled jobs (`@Scheduled`). Disabled in tests via `app.scheduling.enabled=false`. |

#### `common/entity/BaseEntity.java`
Parent class for most entities. Gives every table: `id` (UUID, auto-generated), `createdAt`,
`updatedAt` (auto-filled). Entities write `extends BaseEntity` to get these.

#### `common/exception/`: errors

| File | Use it when | HTTP |
|---|---|---|
| `ErrorCode.java` | Enum of all error codes + their HTTP status. | – |
| `ApiException.java` | Parent of all our errors. | – |
| `BadRequestException.java` | Input breaks a business rule ("type doesn't match category"). | 400 |
| `UnauthorizedException.java` | Wrong password, bad refresh token. | 401 |
| `ForbiddenException.java` | Allowed to see, not allowed to change (default categories). | 403 |
| `ResourceNotFoundException.java` | Not found or belongs to another user. | 404 |
| `ConflictException.java` | Duplicate or "in use". | 409 |
| `GlobalExceptionHandler.java` | **Catches every exception** from controllers and turns it into the standard JSON error (`ApiError`). Also handles validation errors, bad JSON, bad sort field, DB constraint errors, and unknown errors (500, logged). | – |

So in a service you only write `throw new ConflictException("...")` and the client gets a proper
409 JSON automatically.

#### `common/response/`

| File | Does |
|---|---|
| `ApiResponse.java` | Shape of every **success** JSON: `success: true, status, message, data, timestamp, path`. Controllers return `ApiResponse.ok("message", data)` / `ApiResponse.created(...)` / `ApiResponse.ok("message")`. |
| `ApiError.java` | Shape of every **error** JSON: `success: false, status, code, message, fieldErrors, timestamp, path`. |
| `PageResponse.java` | Shape of every paged list: `content, page, size, totalElements, totalPages`. `PageResponse.of(page, mapper)` builds it. |

#### `common/security/`: login and protection

| File | Does |
|---|---|
| `SecurityConfig.java` | **The security rulebook.** Which URLs are public (register, OTP / password-reset endpoints, login, refresh, health, swagger) and which need a token (everything else). Stateless (no sessions/cookies), CSRF off, adds `JwtAuthFilter`, defines `PasswordEncoder` (BCrypt). |
| `JwtAuthFilter.java` | Runs on **every request**. Reads `Authorization: Bearer <token>`, asks `JwtService` to verify it, and if valid marks the request as logged in with a `CurrentUser`. |
| `JwtService.java` | Creates and verifies JWT access tokens (HS512, signed with `JWT_SECRET`). Token contains user id + email. |
| `JwtProperties.java` | Reads `app.jwt.*` from config into a Java record. |
| `CurrentUser.java` | The logged-in user (`id`, `email`). Controllers get it with `@AuthenticationPrincipal CurrentUser user`. |
| `SecurityErrorHandler.java` | Writes 401/403 JSON when the request fails **before** reaching a controller (no/invalid token). |

#### `common/mail/`: sending emails

| File | Does |
|---|---|
| `MailService.java` | Interface: `send(to, subject, body)`. Services depend on this, not on SMTP. |
| `SmtpMailService.java` | Real email via Gmail/SMTP (`spring.mail.*`). Used when `MAIL_ENABLED=true`. |
| `LoggingMailService.java` | Prints the email to the server log instead. Used when `MAIL_ENABLED=false` (dev without SMTP). |
| `MailSettings.java` | Reads `app.mail.*` (`enabled`, `from`, `verify-domain`). |
| `EmailDomainChecker.java` | DNS check that an email's domain can receive mail (catches `gmail.con`). |

#### `common/util/HashUtils.java`
`sha256Hex()` (hashes refresh tokens before saving) and `randomToken()` (secure random strings).

---

### 8.3 `auth/`: register + OTP, login, password reset, refresh, logout

| File | Does |
|---|---|
| `AuthController.java` | `POST /api/v1/auth/register`, `/verify-email`, `/resend-otp`, `/login`, `/forgot-password`, `/verify-reset-otp`, `/reset-password`, `/refresh`, `/logout`. |
| `AuthService.java` | Register (lowercase email, check duplicate, BCrypt password, send OTP), verify email, resend OTP, forgot/reset password, login (check password + verified), refresh (rotate token, detect re-use → revoke all), logout (revoke). `issueTokens()` creates both tokens. |
| `RefreshToken.java` | Entity for `refresh_tokens` (stores only the **hash** of the token). |
| `RefreshTokenRepository.java` | Find by hash, revoke all for a user, delete expired. |
| `RegistrationService.java` | Sign-up: checks email (format, domain, duplicate), emails the code, keeps details in `pending_registrations`, creates the real `User` only when the code is verified. |
| `PendingRegistration.java` / `PendingRegistrationRepository.java` | Entity + queries for `pending_registrations`. |
| `OtpService.java` | Creates 6-digit codes (stores only a hash), emails them, checks them (5 tries, 10 min, 60 s resend cooldown), creates/consumes password-reset tokens. |
| `OtpCode.java` / `OtpCodeRepository.java` / `OtpPurpose.java` | Entity for `otp_codes`, its queries, and the two purposes (`EMAIL_VERIFICATION`, `PASSWORD_RESET`). |
| `OtpProperties.java` | Reads `app.otp.*` (length, ttl, max-attempts, resend-cooldown, reset-token-ttl). |
| `AuthCleanupJob.java` | Every day at 03:00 deletes old expired refresh tokens and OTP codes. |
| `dto/` | Input: `RegisterRequest`, `VerifyOtpRequest`, `ResendOtpRequest`, `ForgotPasswordRequest`, `ResetPasswordRequest`, `LoginRequest`, `RefreshRequest`. Output: `AuthResponse` (tokens), `MessageResponse`, `ResetTokenResponse`. |

**Sign-up flow:** register (only a *pending* sign-up is saved) → user gets a 6-digit code by email → `/verify-email` with the code → account is created → tokens.
**Forgot password:** `/forgot-password` → code by email → `/verify-reset-otp` → `resetToken` → `/reset-password`.

**Login flow:** app sends email+password → gets `accessToken` (15 min) + `refreshToken` (7 days) →
sends `accessToken` on every request → when it expires, calls `/auth/refresh` to get a new pair.

### 8.4 `user/`: profile

| File | Does |
|---|---|
| `User.java` | Entity for `users`: email, password (hash), fullName, currencyCode. |
| `UserRepository.java` | `findByEmail`, `existsByEmail`. |
| `UserService.java` | Get/update profile, change password (also logs out all devices). `getReference()` is used by other features to attach the owner to new rows. |
| `UserController.java` | `GET/PUT /api/v1/users/me`, `PUT /api/v1/users/me/password`. |
| `UserMapper.java` | `User` → `UserResponse` (never exposes the password). |
| `dto/` | `UserResponse`, `UpdateProfileRequest`, `ChangePasswordRequest`. |

### 8.5 `category/`

| File | Does |
|---|---|
| `Category.java` | Entity for `categories`. `user == null` means system default. |
| `CategoryType.java` | Enum `EXPENSE`, `INCOME`. **Also used by transactions.** |
| `CategoryRepository.java` | `findVisibleToUser` (defaults + own), `existsVisibleByName` (duplicate check). |
| `CategoryService.java` | list/create/update/delete with rules: defaults read-only (403), duplicate name (409), in use (409). `getUsable()` is used by transactions and budgets to load a category safely. |
| `CategoryController.java` | `/api/v1/categories` CRUD. |
| `CategoryMapper.java`, `dto/` | `CategoryRequest`, `CategoryResponse`. |

### 8.6 `transaction/`

| File | Does |
|---|---|
| `Transaction.java` | Entity for `transactions`: user, category, type, amount (`BigDecimal`), note, transactionDate (`LocalDate`). |
| `TransactionRepository.java` | `findByIdAndUserId` (safe lookup), `findAll(spec, page)` with category pre-loaded, `sumByCategory` (used by budgets). |
| `TransactionSpecifications.java` | Builds the WHERE clause from filters: always `user = me`, plus optional type, category, date range, amount range, note search. |
| `TransactionService.java` | Create/update (category must be usable, type must match), list (validates filters, default sort, allowed sort fields), get, delete. |
| `TransactionController.java` | `/api/v1/transactions` CRUD + list. |
| `TransactionMapper.java`, `dto/` | Request/response/filter records. |

### 8.7 `budget/`

| File | Does |
|---|---|
| `Budget.java` | Entity for `budgets`: user, category (null = overall), amount, year, month. |
| `BudgetRepository.java` | Find by month, find by id+user, duplicate check `existsForPeriod`. |
| `BudgetService.java` | CRUD + computes **spent** (sum of that month's expenses, from `TransactionRepository.sumByCategory`) and **remaining**. Only EXPENSE categories allowed. |
| `BudgetController.java` | `/api/v1/budgets` (list needs `?year=&month=`). |
| `BudgetMapper.java`, `dto/` | `BudgetRequest`, `BudgetResponse` (includes spent/remaining). |

### 8.8 `report/` (read-only, no table of its own)

| File | Does |
|---|---|
| `ReportRepository.java` | Three `GROUP BY` queries written in JPQL with `EntityManager`: totals by type, expenses by category, totals by month. |
| `ReportService.java` | Turns query rows into answers: balance, percentages (2 decimals), always 12 months. |
| `ReportController.java` | `/api/v1/reports/summary`, `/by-category`, `/monthly`. |
| `dto/` | `SummaryResponse`, `CategoryBreakdownItem`, `MonthlyTrendItem`. |

### Who calls whom

```
AuthService ─────► UserRepository, RefreshTokenRepository, JwtService
UserService ─────► UserRepository, RefreshTokenRepository
CategoryService ─► CategoryRepository, TransactionRepository, BudgetRepository, UserService
TransactionService ► TransactionRepository, CategoryService, UserService
BudgetService ───► BudgetRepository, TransactionRepository, CategoryService, UserService
ReportService ───► ReportRepository
```

---

## 9. Journey of one request (file by file)

**Request:** the app creates an expense.

```http
POST /api/v1/transactions
Authorization: Bearer eyJhbGciOi...
Content-Type: application/json

{ "type": "EXPENSE", "amount": 250.50, "categoryId": "2678...", "transactionDate": "2026-11-05", "note": "Lunch" }
```

| # | File | What happens |
|---|---|---|
| 1 | `common/security/JwtAuthFilter.java` | Reads the Bearer token → `JwtService.parse()` → valid → sets `CurrentUser(id, email)`. |
| 2 | `common/security/SecurityConfig.java` | URL needs login → user is logged in → allowed. (No token → `SecurityErrorHandler` returns 401.) |
| 3 | `transaction/TransactionController.java` | `@PostMapping` matches. JSON → `CreateTransactionRequest`. `@Valid` checks `amount > 0`, max 2 decimals, required fields. (Invalid → `GlobalExceptionHandler` → 400 with `fieldErrors`.) |
| 4 | `transaction/TransactionService.create()` | Calls `CategoryService.getUsable()` (category must be default or mine, else 404). Checks `type == category.type` (else 400). Gets the `User` via `UserService.getReference()`. Builds a `Transaction`. |
| 5 | `transaction/TransactionRepository.save()` | Hibernate runs `INSERT INTO transactions ...`. `BaseEntity` fills `id`, `createdAt`, `updatedAt`. |
| 6 | `transaction/TransactionMapper.toResponse()` | Entity → `TransactionResponse`. |
| 7 | Controller | `return ApiResponse.created("Transaction created successfully", dto)` + `@ResponseStatus(CREATED)` → **201** with `{ success, status, message, data, timestamp, path }`. |

Every other endpoint follows the same path; only the feature folder changes.

**Rule for new endpoints:** a controller method always returns `ApiResponse<T>`:
`ApiResponse.ok("X fetched successfully", data)`, `ApiResponse.created("X created successfully", data)` (with
`@ResponseStatus(HttpStatus.CREATED)`), or `ApiResponse.ok("X deleted successfully")` when there is no data.
Errors: just `throw` an `ApiException` subclass with an exact message; `GlobalExceptionHandler` builds the `ApiError`.

---

## 10. `src/test`: the tests

```
src/test/java/com/example/expensetracker/
├── support/
│   ├── AbstractIntegrationTest.java   Base class: starts the full app + real PostgreSQL,
│   │                                  wipes user data before each test
│   ├── TestApi.java                   Helpers: register() (incl. OTP verify), createCategory(), createTransaction(), ...
│   └── CapturingMailService.java      Fake email: tests read OTP codes from it
├── ExpenseTrackerApplicationTests.java  App starts, migrations work, 13 defaults exist
├── auth/        AuthIntegrationTest, UserProfileIntegrationTest
├── category/    CategoryIntegrationTest, CategoryServiceTest (unit)
├── transaction/ TransactionIntegrationTest, TransactionServiceTest (unit)
├── budget/      BudgetIntegrationTest
└── report/      ReportIntegrationTest
src/test/resources/application-test.yml  Test-only config (fixed JWT secret, scheduling off)
```

- **Integration test** = real HTTP call (MockMvc) through all layers into a real DB.
- **Unit test** (`*ServiceTest`) = one service with fake (mocked) repositories; very fast.
- Which DB? If Docker is running, a temporary PostgreSQL container (Testcontainers). If not,
  your local `expense_tracker_test` database. **Your dev database is never touched.**
- Run: `./mvnw clean verify`. One class: `./mvnw test -Dtest=BudgetIntegrationTest`.

---

## 11. Other folders

| Folder | What is inside | Use |
|---|---|---|
| `.agent/` | `prd.md` (requirements), `rules.md` (order + rules + decisions), `project.md` (structure plan) | Read before adding features. Update `prd.md` when requirements change. |
| `docs/` | This guide + SETUP, ARCHITECTURE, DATABASE, API | Update `API.md` when you add/change an endpoint, `DATABASE.md` when you add a migration. |
| `postman/` | Collection (all requests, auto-saves tokens/ids) + local environment | Import into Postman. Add new requests when you add endpoints, then *Export* and overwrite the file. |
| `scripts/` | `setup-local-db.sh` (run once per machine), `dev.sh` (daily: start server with auto-restart) | See `docs/DEVELOPMENT.md`. |
| `.github/workflows/` | `ci.yml` | Runs tests on GitHub. |
| `target/` | Build output (compiled classes, jar). Git-ignored. | Delete anytime; `./mvnw clean` does it. |

---

## 12. How to change things (recipes)

### 12.1 Add a new field to an existing table
*Example: add `payment_method` to transactions.*

1. **Migration**: create `src/main/resources/db/migration/V7__add_payment_method_to_transactions.sql`
   ```sql
   ALTER TABLE transactions ADD COLUMN payment_method VARCHAR(20);
   ```
2. **Entity** `transaction/Transaction.java`:
   ```java
   @Column(name = "payment_method", length = 20)
   private String paymentMethod;
   ```
3. **Request DTOs** `dto/CreateTransactionRequest.java`, `UpdateTransactionRequest.java`: add
   `@Size(max = 20) String paymentMethod`.
4. **Response DTO** `dto/TransactionResponse.java`: add `String paymentMethod`.
5. **Mapper** `TransactionMapper.java`: pass `t.getPaymentMethod()`.
6. **Service** `TransactionService.create/update`: set the field.
7. **Test**: add an assertion in `TransactionIntegrationTest`.
8. **Docs**: `docs/API.md`, `docs/DATABASE.md`, Postman body.
9. `./mvnw clean verify`.

### 12.2 Change the database schema (any change)
- Always a **new** file `V{next}__what_it_does.sql`. Check the last number in `db/migration/`.
- Never edit `V1..V8` (Flyway will fail with "checksum mismatch").
- Then update the Entity; if they don't match, the app won't start (that's good, it tells you).

### 12.3 Add a new endpoint to an existing feature
*Example: `GET /api/v1/transactions/recent`.*

1. Repository: add a query method if needed.
2. Service: add the method (filter by `userId`!), mark `@Transactional(readOnly = true)` for reads.
3. Controller: add `@GetMapping("/recent")` with `@AuthenticationPrincipal CurrentUser user`.
4. Test + `docs/API.md` + Postman.

> Public (no login) endpoint? Also add its path to `PUBLIC_POST`/`PUBLIC_GET` in
> `common/security/SecurityConfig.java`. Otherwise it is protected automatically.

### 12.4 Add a whole new feature
*Example: "savings goals".*

1. Migration `V7__create_savings_goals_table.sql` (UUID id, `user_id` FK, `created_at`, `updated_at`).
2. New package `com.example.expensetracker.goal/` copying the shape of `budget/`:
   `SavingsGoal.java` (extends `BaseEntity`), `SavingsGoalRepository`, `SavingsGoalService`,
   `SavingsGoalController` (`/api/v1/goals`), `SavingsGoalMapper`, `dto/`.
3. In the service, **every** lookup uses the user id (`findByIdAndUserId`).
   Dependencies go in as `private final` fields + a constructor (copy the pattern from `BudgetService`).
   Entities need a `protected` no-arg constructor (JPA) and getters/setters (IntelliJ: *Code → Generate*).
4. Tests in `src/test/java/.../goal/` extending `AbstractIntegrationTest`, including an isolation test
   (user B can't see user A's goal).
5. Docs + Postman + `.agent/prd.md`.

### 12.5 Add a new business rule / error
In the service: `throw new BadRequestException("...")` (400), `ConflictException` (409),
`ForbiddenException` (403), `ResourceNotFoundException` (404). No other code needed;
`GlobalExceptionHandler` formats it. Need a new kind of error? Add a value in `ErrorCode.java`
and a small class extending `ApiException`.

### 12.6 Add a validation rule on input
Annotations on the DTO record fields: `@NotBlank`, `@Size(max=)`, `@Email`, `@Min`, `@Max`,
`@Pattern`, `@DecimalMin`, `@Digits`. Make sure the controller parameter has `@Valid`.

### 12.7 Add a new default category
New migration `V7__add_default_category_travel.sql`:
```sql
INSERT INTO categories (id, user_id, name, type, icon, color, created_at, updated_at)
VALUES (gen_random_uuid(), NULL, 'Travel', 'EXPENSE', 'flight', '#03A9F4', now(), now());
```
(Then update the "13 defaults" counts in tests.)

### 12.8 Common settings

| Change | Where |
|---|---|
| Token lifetimes | `.env`: `JWT_ACCESS_TTL=30m`, `JWT_REFRESH_TTL=30d` |
| Port | `.env`: `PORT=9090` |
| Allowed web origins | `.env`: `CORS_ALLOWED_ORIGINS=https://myapp.com` |
| Default/max page size | `application.yml` → `spring.data.web.pageable` |
| Sortable transaction fields | `TransactionService.SORTABLE` |
| Log level | `application-dev.yml` → `logging.level` |
| Add a library | `pom.xml` `<dependencies>`, then rerun `./mvnw` |
| Run as production locally | `SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run` |

### 12.9 Rename the base package (e.g. to your company name)
Rename `com/example/expensetracker` in both `src/main/java` and `src/test/java` with your IDE's
"Refactor → Rename package" (IntelliJ: right-click package). Also update `<groupId>` in `pom.xml`
and the `logging.level.com.example.expensetracker` line in `application-dev.yml`.

---

## 13. Daily commands cheat sheet

```bash
# first time on a machine
./scripts/setup-local-db.sh
cp .env.example .env            # and set JWT_SECRET

# run the app with auto-restart (http://localhost:8080), like npm run dev
./scripts/dev.sh

# run the app without auto-restart
./mvnw spring-boot:run

# run all tests
./mvnw clean verify

# run one test class
./mvnw test -Dtest=TransactionIntegrationTest

# build a jar (target/expense-tracker-0.0.1-SNAPSHOT.jar) and run it
./mvnw clean package -DskipTests
java -jar target/expense-tracker-0.0.1-SNAPSHOT.jar

# open the database
psql -h localhost -U expense -d expense_tracker          # password: expense

# start/stop local PostgreSQL (Homebrew)
brew services start postgresql@16
brew services stop postgresql@16

# reset dev DB (deletes ALL data; tables come back on next app start)
psql -h localhost -U expense -d expense_tracker -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"
```

| URL | What |
|---|---|
| http://localhost:8080/swagger-ui.html | Try every endpoint in the browser (click **Authorize**, paste the access token) |
| http://localhost:8080/actuator/health | Is the app + DB up? |

---

## 14. Rules: what NOT to do

| Don't | Why | Do instead |
|---|---|---|
| Edit an old migration file | Flyway checksum error; other machines break | New `V{n+1}__...sql` |
| Use `double`/`float` for money | Rounding errors (0.1 + 0.2 ≠ 0.3) | `BigDecimal` |
| Query by id only (`findById`) for user data | User A could read user B's data | `findByIdAndUserId` |
| Return an Entity from a controller | Leaks fields (password!), lazy-loading errors | Return a DTO via the Mapper |
| Put business rules in a controller | Hard to test and reuse | Put them in the Service |
| Write SQL in a service | Mixes layers | Repository method |
| Commit `.env` or secrets | Passwords leak | Keep in `.env` (ignored), document in `.env.example` |
| Set `ddl-auto: update` | Hibernate would change tables silently | Flyway migrations only |
| Catch exceptions and return `null` | Hides bugs | Throw an `ApiException` subclass |
| Skip tests after a change | Something else may break | `./mvnw clean verify` before every commit |

---

**Next reading:** `docs/DEVELOPMENT.md` (running + workflow), `docs/API.md` (endpoint details), `docs/DATABASE.md` (tables),
`docs/ARCHITECTURE.md` (deeper design), `docs/SETUP.md` (install problems).
