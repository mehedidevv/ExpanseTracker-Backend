expense-tracker/
├── .agent/
│   ├── prd.md                          # product requirements
│   ├── rules.md                        # execution order + engineering rules
│   └── project.md                      # this file (folder structure)
├── .github/
│   └── workflows/
│       └── ci.yml                      # build + test on push/PR
├── docs/                               # SETUP, ARCHITECTURE, DATABASE, API
├── postman/                            # collection + local environment
├── scripts/
│   └── setup-local-db.sh               # creates role + dev/test databases
├── docker-compose.yml                  # local PostgreSQL
├── Dockerfile                          # multi-stage production image
├── .env.example                        # documented env vars (no real secrets)
├── .gitignore
├── README.md
├── pom.xml
├── mvnw, mvnw.cmd, .mvn/
└── src/
├── main/
│   ├── java/com/example/expensetracker/
│   │   ├── ExpenseTrackerApplication.java    # MUST stay in the root package
│   │   │
│   │   ├── common/                           # shared, feature-agnostic code
│   │   │   ├── config/                       # OpenApiConfig, CorsConfig, JpaAuditingConfig
│   │   │   ├── exception/                    # GlobalExceptionHandler, ApiException,
│   │   │   │                                 # ResourceNotFoundException, ConflictException
│   │   │   ├── response/                     # ApiError, PageResponse
│   │   │   ├── security/                     # SecurityConfig, JwtService, JwtAuthFilter,
│   │   │   │                                 # CurrentUser helper
│   │   │   ├── entity/                       # BaseEntity (id, createdAt, updatedAt)
│   │   │   ├── mail/                         # MailService (SMTP or log)
│   │   │   └── util/                         # small, stateless helpers only
│   │   │
│   │   ├── auth/
│   │   │   ├── AuthController.java
│   │   │   ├── AuthService.java
│   │   │   ├── RefreshToken.java             # entity
│   │   │   ├── RefreshTokenRepository.java
│   │   │   ├── OtpCode.java / OtpCodeRepository.java / OtpService.java  # email OTP
│   │   │   ├── RegistrationService.java / PendingRegistration*.java      # sign-up until OTP verified
│   │   │   ├── AuthCleanupJob.java           # purges expired tokens + codes
│   │   │   └── dto/                          # RegisterRequest, LoginRequest,
│   │   │                                     # RefreshRequest, AuthResponse
│   │   │
│   │   ├── user/
│   │   │   ├── UserController.java
│   │   │   ├── UserService.java
│   │   │   ├── UserRepository.java
│   │   │   ├── User.java                     # entity
│   │   │   ├── UserMapper.java
│   │   │   └── dto/
│   │   │
│   │   ├── category/
│   │   │   ├── CategoryController.java
│   │   │   ├── CategoryService.java
│   │   │   ├── CategoryRepository.java
│   │   │   ├── Category.java
│   │   │   ├── CategoryType.java             # enum EXPENSE, INCOME
│   │   │   ├── CategoryMapper.java
│   │   │   └── dto/
│   │   │
│   │   ├── transaction/
│   │   │   ├── TransactionController.java
│   │   │   ├── TransactionService.java
│   │   │   ├── TransactionRepository.java
│   │   │   ├── TransactionSpecifications.java  # dynamic filters
│   │   │   ├── Transaction.java
│   │   │   ├── TransactionMapper.java
│   │   │   └── dto/                          # CreateTransactionRequest,
│   │   │                                     # UpdateTransactionRequest,
│   │   │                                     # TransactionResponse, TransactionFilter
│   │   │
│   │   ├── budget/
│   │   │   ├── BudgetController.java
│   │   │   ├── BudgetService.java
│   │   │   ├── BudgetRepository.java
│   │   │   ├── Budget.java
│   │   │   ├── BudgetMapper.java
│   │   │   └── dto/
│   │   │
│   │   └── report/                           # read-only, no entity of its own
│   │       ├── ReportController.java
│   │       ├── ReportService.java
│   │       ├── ReportRepository.java         # aggregate queries
│   │       └── dto/
│   │
│   └── resources/
│       ├── application.yml                   # shared config
│       ├── application-dev.yml               # local dev (Swagger on, SQL logs)
│       ├── application-prod.yml              # production (Swagger off)
│       └── db/migration/                     # Flyway only
│           ├── V1__create_users_table.sql
│           ├── V2__create_refresh_tokens_table.sql
│           ├── V3__create_categories_table.sql
│           ├── V4__seed_default_categories.sql
│           ├── V5__create_transactions_table.sql
│           ├── V6__create_budgets_table.sql
│           ├── V7__email_verification_and_otp_codes.sql
│           └── V8__pending_registrations.sql
│
└── test/
├── java/com/example/expensetracker/    # mirrors main structure
│   ├── support/                          # AbstractIntegrationTest (Testcontainers),
│   │                                     # test data builders
│   ├── auth/
│   ├── category/
│   ├── transaction/
│   ├── budget/
│   └── report/
└── resources/
└── application-test.yml