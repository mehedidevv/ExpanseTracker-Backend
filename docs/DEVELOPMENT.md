# Development Workflow: Running the Server and Making Changes

> Coming from Node.js? This page is your `npm run dev` guide.
> It covers how to start and stop the server, what to do after each kind of change, and the daily routine.

---

## 1. Node.js → this project (cheat sheet)

| Node.js | This project | What it does |
|---|---|---|
| `npm install` | *(automatic)* `./mvnw` downloads libraries on first run | Install dependencies (listed in `pom.xml`, like `package.json`) |
| `npm run dev` (nodemon) | **`./scripts/dev.sh`** | Start server + auto-restart on code change |
| `npm start` | `./mvnw spring-boot:run` | Start server (no file watching) |
| `npm test` | `./mvnw test` | Run tests |
| `npm run build` | `./mvnw clean package` | Build the app → `target/expense-tracker-0.0.1-SNAPSHOT.jar` |
| `node dist/index.js` | `java -jar target/expense-tracker-0.0.1-SNAPSHOT.jar` | Run the built app |
| `package.json` | `pom.xml` | Dependencies + project info |
| `node_modules/` | `~/.m2/repository/` | Downloaded libraries (shared by all projects) |
| `dist/` | `target/` | Build output |
| `.env` + `dotenv` | `.env` (loaded automatically) | Local settings and secrets |
| `nodemon` | Spring Boot **DevTools** | Restart on change |

`./mvnw` is the **Maven wrapper**. Always use `./mvnw` (not `mvn`); it downloads the right Maven version for you.

---

## 2. Before the first run (once per machine)

```bash
./scripts/setup-local-db.sh            # creates DB user + databases in your local PostgreSQL
cp .env.example .env                   # then put a value in JWT_SECRET:
openssl rand -base64 48                # copy this output into JWT_SECRET= in .env
```

PostgreSQL must be running:
```bash
brew services start postgresql@16      # start (it then auto-starts at login)
brew services list                     # check status
```

---

## 3. Start the server

### Option A: Dev mode with auto-restart (recommended, like `npm run dev`)

```bash
./scripts/dev.sh
```

What it does:
1. Compiles the code (stops with an error message if it doesn't compile).
2. Starts the server on **http://localhost:8080** with the `dev` profile.
3. Checks `src/main/` and `pom.xml` for changes every second. When you save a file, it recompiles and
   **DevTools restarts the app in about 1 second**.
4. If your change has a compile error, the server **keeps running the last good version** and prints
   `✗ Compile error`. Fix the error and save again.
5. **Ctrl+C** stops the server.

What you'll see:
```
▶ Compiling...
▶ Starting server (Ctrl+C to stop)...
... Started ExpenseTrackerApplication in 2.4 seconds
↻ Change detected, recompiling...
✓ Compiled. Server is restarting...
... Started ExpenseTrackerApplication in 0.5 seconds
```

### Option B: Plain start (no watching)

```bash
./mvnw spring-boot:run
```
After a change you must stop it (Ctrl+C) and run it again.

### Option C: From IntelliJ (Run button)

1. Open `src/main/java/com/example/expensetracker/ExpenseTrackerApplication.java`.
2. Click the green ▶ next to `public class ExpenseTrackerApplication` → **Run**
   (or **Debug** to use breakpoints).
3. The **Run** panel at the bottom shows the logs. ■ (red square) stops the server.

The `.env` file is read automatically, so no extra IntelliJ setup is needed.

**Auto-restart in IntelliJ** (DevTools restarts when compiled classes change):
- Quick way: after editing, press **⌘ F9** (*Build → Build Project*). The app restarts in about 1 s.
- Fully automatic:
  1. *Settings → Build, Execution, Deployment → Compiler* → tick **Build project automatically**.
  2. *Settings → Advanced Settings* → tick **Allow auto-make to start even if developed application is currently running**.

> Use **one** way at a time. Two servers can't both use port 8080.

### Is it running?

| Check | Expected |
|---|---|
| Log line | `Started ExpenseTrackerApplication in X seconds` |
| http://localhost:8080/actuator/health | `{"status":"UP"}` |
| http://localhost:8080/swagger-ui.html | Swagger page with all endpoints |

---

## 4. Stop the server

- In the terminal where it runs: **Ctrl+C**
- In IntelliJ: the red ■ button
- Server stuck or running in another window ("Port 8080 already in use"):
  ```bash
  lsof -ti tcp:8080 | xargs kill
  ```

---

## 5. I changed something: what now?

| What you changed | Where | What to do |
|---|---|---|
| Java code (controller, service, entity, DTO...) | `src/main/java/**` | **dev.sh**: nothing, just save. **IntelliJ**: ⌘F9. **Plain run**: Ctrl+C and start again. |
| Config | `src/main/resources/application*.yml` | Same as Java code (auto-restart). |
| New DB migration | `src/main/resources/db/migration/V7__*.sql` | Same as Java code. Flyway applies it on restart; check the log for `Migrating schema "public" to version "7..."`. If you also changed an Entity, save both before the restart finishes. |
| Settings or secrets | `.env` | **Restart manually** (Ctrl+C, start again). `.env` is not watched. |
| Library added or removed | `pom.xml` | **Restart manually**. In IntelliJ also click **Maven panel → 🔄 Reload**. |
| Tests | `src/test/**` | Run `./mvnw test` (server restart not needed). |
| Docs / Postman | `docs/`, `postman/` | Nothing. |

### When auto-restart is not enough

DevTools reloads **your** classes quickly, but a few things need a full stop + start:
- `.env` or `pom.xml` changes
- the app behaves strangely after many restarts
- changes to `ExpenseTrackerApplication.java` annotations

```bash
# full clean restart
Ctrl+C
./mvnw clean            # deletes target/ (like deleting dist/)
./scripts/dev.sh
```

---

## 6. Daily routine

```bash
# 1. get latest code
git pull

# 2. start dev mode (applies any new migrations from teammates automatically)
./scripts/dev.sh

# 3. code → save → test in Postman / Swagger → repeat

# 4. before committing: run the full test suite in a second terminal
./mvnw clean verify            # must end with BUILD SUCCESS

# 5. commit
git add -A && git commit -m "feat: ..."
```

### Checklist before every commit
- [ ] `./mvnw clean verify` → **BUILD SUCCESS**
- [ ] New/changed endpoint → `docs/API.md` + Postman collection updated
- [ ] New migration → `docs/DATABASE.md` updated
- [ ] No secrets in code (they go in `.env`; document the name in `.env.example`)

---

## 7. Testing commands

```bash
./mvnw test                                         # all tests (unit + integration)
./mvnw clean verify                                 # full build + all tests (what CI runs)
./mvnw test -Dtest=TransactionIntegrationTest       # one test class
./mvnw test -Dtest=TransactionIntegrationTest#updateAndDelete   # one test method
```
In IntelliJ: open a test class and click the green ▶ next to the class or a method.

Tests use the separate `expense_tracker_test` database (or Docker if it is running),
so **your dev data is never touched**. The server does not need to be running for tests.

---

## 8. Seeing what's happening (logs and debugging)

| Need | How |
|---|---|
| See SQL queries | Already on in dev (`org.hibernate.SQL: debug` in `application-dev.yml`). |
| Log from your code | In a class: `private static final Logger log = LoggerFactory.getLogger(MyClass.class);` then `log.info("value = {}", value);` |
| More/less logging | `application-dev.yml` → `logging.level.com.example.expensetracker: debug` (or `info`) |
| Breakpoints | IntelliJ: click the line number gutter → run with **Debug** (🐞) instead of Run. |
| Debug while using dev.sh | `./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"` then IntelliJ *Run → Attach to Process*. Easier: just use IntelliJ's Debug button. |
| Look at the DB | `psql -h localhost -U expense -d expense_tracker` (password `expense`), or DBeaver/TablePlus. |

---

## 9. Build and run like production

```bash
./mvnw clean package -DskipTests                                   # build the jar
SPRING_PROFILES_ACTIVE=prod java -jar target/expense-tracker-0.0.1-SNAPSHOT.jar
```
- DevTools is **automatically excluded** from the jar, so there's no auto-restart in production.
- `prod` profile: Swagger off, JSON logs.
- Docker image: `docker build -t expense-tracker .` (see `docs/SETUP.md` §7).

---

## 10. Problems and fixes

| Problem | Fix |
|---|---|
| `Port 8080 is already in use` | `lsof -ti tcp:8080 \| xargs kill`, or use another port: `PORT=8081 ./scripts/dev.sh` |
| `Connection to localhost:5432 refused` | PostgreSQL not running: `brew services start postgresql@16` |
| `JWT_SECRET must be at least 32 bytes` | Set `JWT_SECRET` in `.env` (§2). |
| Change not showing up | dev.sh: look for `✗ Compile error` in the terminal. IntelliJ: press ⌘F9. Still nothing: full clean restart (§5). |
| Red errors in IntelliJ but `./mvnw compile` works | Maven panel → 🔄 Reload; then *File → Invalidate Caches → Restart*. |
| `Schema-validation: missing column` | Entity has a field with no matching migration. Add a `V{n+1}__*.sql`. |
| `Migration checksum mismatch` | An already-applied migration was edited. Undo that edit and create a new migration instead. |
| App restarts in a loop | Something keeps writing into `src/main` (e.g. a generated file). Stop dev.sh and check `git status`. |
| Dependencies won't download | Check internet, then `./mvnw -U clean compile` (forces a re-download). |
