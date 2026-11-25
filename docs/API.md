# API Reference

Base URL: `http://localhost:8080/api/v1` · Interactive docs: `/swagger-ui.html` (dev profile)
· Postman: `postman/expense-tracker.postman_collection.json`

Each endpoint below lists everything you need in Postman: **method + URL**, the **Authorization** tab,
**headers**, **body**, the response, and the errors.

---

## 1. Postman setup (do this once)

### 1.1 Import
1. Postman → **Import** → select both files in `postman/`:
   - `expense-tracker.postman_collection.json` (all requests)
   - `expense-tracker.local.postman_environment.json` (`baseUrl = http://localhost:8080/api/v1`)
2. Top-right environment dropdown → select **Expense Tracker - Local**.

### 1.2 Variables
Write `{{name}}` in a URL, header or body and Postman replaces it with the variable's value.
These are **collection variables** (collection → *Variables* tab):

| Variable | Set by | Used in |
|---|---|---|
| `baseUrl` | environment | every URL |
| `email`, `password` | you (defaults are filled in) | register / login bodies |
| `otp` | **you**: paste the 6-digit code from the email | verify-email, verify-reset-otp |
| `accessToken` | auto (script after verify-email / login / refresh) | `Authorization: Bearer` header |
| `refreshToken` | auto (same) | refresh, logout bodies |
| `resetToken` | auto (script after verify-reset-otp) | reset-password body |
| `foodCategoryId`, `salaryCategoryId`, `categoryId`, `transactionId`, `budgetId` | auto (scripts) | ids in URLs / bodies |
| `year`, `month` | you | budgets, reports |

### 1.3 Authorization tab (where the token goes)
There are only two kinds of endpoints:

| Endpoint type | Authorization tab | What Postman sends |
|---|---|---|
| **Public** 🔓 (register, OTP, login, refresh, forgot/reset password) | **No Auth** | nothing |
| **Protected** 🔒 (everything else) | **Bearer Token** → Token: `{{accessToken}}` | header `Authorization: Bearer eyJhbGciOi...` |

In the imported collection the Bearer token is set **once at collection level**
(collection → *Authorization* tab → Bearer Token → `{{accessToken}}`).
Every protected request uses *Inherit auth from parent*, so you never copy the token by hand.
Public requests are set to *No Auth*.

> Making a new request yourself? Choose **Authorization → Inherit auth from parent** (protected)
> or **No Auth** (public). Or add the header manually: `Authorization` = `Bearer {{accessToken}}`
> (one space after `Bearer`).

### 1.4 Headers

| Header | When | Value |
|---|---|---|
| `Content-Type` | every request that has a body (POST / PUT) | `application/json` |
| `Authorization` | protected endpoints (added automatically by the Authorization tab) | `Bearer {{accessToken}}` |

GET and DELETE need no `Content-Type`. Postman adds `Content-Type` for you when you pick
**Body → raw → JSON**.

### 1.5 Body
**Body** tab → **raw** → dropdown **JSON** → paste the JSON shown for the endpoint.

### 1.6 Saving tokens automatically (Scripts → Post-response)
The collection already contains these scripts. To add one to your own request, use the request's
**Scripts → Post-response** tab:

```javascript
// after verify-email / login / refresh
const j = pm.response.json().data;      // values are inside "data"
pm.collectionVariables.set("accessToken", j.accessToken);
pm.collectionVariables.set("refreshToken", j.refreshToken);
```

### 1.7 Token lifetime
- `accessToken` lasts **15 minutes**. A protected request then returns `401`. Run **Refresh token**
  (or **Login**) and the new token is saved automatically.
- `refreshToken` lasts **7 days** and works only once (each refresh returns a new one).

### 1.8 Typical order
```
Register → (check email, put the code in {{otp}}) → Verify Email   ← accessToken saved
→ Categories → Transactions → Budgets → Reports → Logout
Forgot password → (code in {{otp}}) → Verify reset OTP → Reset password → Login
```

---

## 2. Conventions

- JSON, `camelCase`. Dates `YYYY-MM-DD`. Timestamps ISO-8601 UTC (`2026-11-25T04:25:50Z`).
- Money is a JSON number with up to 2 decimals (`250.50`). Must be `> 0`.
- Status codes: `200` OK, `201` created, `400/401/403/404/405/409/415/429/500/503` errors.
  Every response has a JSON body: delete/logout/password changes return `200` + a message.
- Someone else's resource id returns `404`.

### Response format (every endpoint)

**Success**: the endpoint's result is inside `data`:
```json
{
  "success": true,
  "status": 200,
  "message": "Login successful",
  "data": { "accessToken": "eyJhbGciOi...", "refreshToken": "2yQ1b...", "tokenType": "Bearer", "expiresIn": 900 },
  "timestamp": "2026-11-25T10:07:27.926Z",
  "path": "/api/v1/auth/login"
}
```
When there is nothing to return (register, resend, logout, delete, password changes) `data` is left out:
```json
{ "success": true, "status": 200, "message": "Logged out successfully",
  "timestamp": "2026-11-25T10:07:28.146Z", "path": "/api/v1/auth/logout" }
```
**Error**: same envelope with `success: false`, a machine-readable `code`, and `fieldErrors` for invalid fields:
```json
{
  "success": false,
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "email must be a valid email address, e.g. name@example.com",
  "fieldErrors": [{ "field": "email", "message": "must be a valid email address, e.g. name@example.com" }],
  "timestamp": "2026-11-25T10:00:00Z",
  "path": "/api/v1/auth/register"
}
```
- `status` in the body always equals the HTTP status code.
- Client rule: `if (body.success) use body.data else show body.message`.
- **In the endpoint sections below, the JSON shown under a status code is the value of `data`.**

### Pagination (`GET /transactions`)
Query: `page` (0-based, default 0), `size` (default 20, max 100), `sort=field,dir` (repeatable).
`data` is:
```json
{ "content": [ ... ], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }
```

### Error codes

`fieldErrors` is omitted when empty. Codes: `VALIDATION_ERROR` (400), `UNAUTHORIZED` (401),
`FORBIDDEN` (403), `EMAIL_NOT_VERIFIED` (403), `NOT_FOUND` (404), `CONFLICT` (409),
`TOO_MANY_REQUESTS` (429), `SERVICE_UNAVAILABLE` (503, email could not be sent), `INTERNAL_ERROR` (500).
Also `METHOD_NOT_ALLOWED` (405, wrong HTTP method) and `UNSUPPORTED_MEDIA_TYPE` (415, missing `Content-Type: application/json`).

---

## 3. Endpoint summary

🔓 = No Auth · 🔒 = Bearer `{{accessToken}}` · Body column: ✓ = needs `Content-Type: application/json` + JSON body

| Method | Path | Auth | Body | Status | Success `message` |
|---|---|---|---|---|---|
| POST | `/auth/register` | 🔓 | ✓ | 201 | Verification code sent to {email} |
| POST | `/auth/verify-email` | 🔓 | ✓ | 200 | Email verified successfully. Your account is ready (data: tokens) |
| POST | `/auth/resend-otp` | 🔓 | ✓ | 200 | Verification code sent to {email} |
| POST | `/auth/login` | 🔓 | ✓ | 200 | Login successful (data: tokens) |
| POST | `/auth/refresh` | 🔓 | ✓ | 200 | Token refreshed successfully (data: tokens) |
| POST | `/auth/logout` | 🔒 | ✓ | 200 | Logged out successfully |
| POST | `/auth/forgot-password` | 🔓 | ✓ | 200 | Password reset code sent to {email} |
| POST | `/auth/verify-reset-otp` | 🔓 | ✓ | 200 | Code verified. Use the reset token to set a new password (data: resetToken) |
| POST | `/auth/reset-password` | 🔓 | ✓ | 200 | Password reset successfully. Please log in with your new password |
| GET | `/users/me` | 🔒 | – | 200 | Profile fetched successfully |
| PUT | `/users/me` | 🔒 | ✓ | 200 | Profile updated successfully |
| PUT | `/users/me/password` | 🔒 | ✓ | 200 | Password changed successfully. Please log in again on your other devices |
| GET | `/categories?type=` | 🔒 | – | 200 | Categories fetched successfully |
| POST | `/categories` | 🔒 | ✓ | 201 | Category created successfully |
| PUT | `/categories/{id}` | 🔒 | ✓ | 200 | Category updated successfully |
| DELETE | `/categories/{id}` | 🔒 | – | 200 | Category deleted successfully |
| GET | `/transactions` | 🔒 | – | 200 | Transactions fetched successfully |
| POST | `/transactions` | 🔒 | ✓ | 201 | Transaction created successfully |
| GET | `/transactions/{id}` | 🔒 | – | 200 | Transaction fetched successfully |
| PUT | `/transactions/{id}` | 🔒 | ✓ | 200 | Transaction updated successfully |
| DELETE | `/transactions/{id}` | 🔒 | – | 200 | Transaction deleted successfully |
| GET | `/budgets?year=&month=` | 🔒 | – | 200 | Budgets fetched successfully |
| POST | `/budgets` | 🔒 | ✓ | 201 | Budget created successfully |
| PUT | `/budgets/{id}` | 🔒 | ✓ | 200 | Budget updated successfully |
| DELETE | `/budgets/{id}` | 🔒 | – | 200 | Budget deleted successfully |
| GET | `/reports/summary?from=&to=` | 🔒 | – | 200 | Summary report generated successfully |
| GET | `/reports/by-category?from=&to=` | 🔒 | – | 200 | Category breakdown generated successfully |
| GET | `/reports/monthly?year=` | 🔒 | – | 200 | Monthly trend generated successfully |
| GET | `/actuator/health` (no `/api/v1`) | 🔓 | – | 200 | `{"status":"UP"}` (Spring format, no envelope) |

---

## 4. Auth

### Flows
```
Sign up:          register ──► (email: 6-digit code) ──► verify-email ──► tokens
                     └─ didn't get it? resend-otp
Forgot password:  forgot-password ──► (email: code) ──► verify-reset-otp ──► resetToken
                     └─ resend-otp {purpose: PASSWORD_RESET}         └─► reset-password
```

OTP rules (configurable in `application.yml` → `app.otp`):
- 6 digits, valid **10 minutes**, single use. Only a hash is stored.
- Requesting a new code invalidates the previous one. A new code can be requested once every **60 s** (else `429`).
- **5** wrong guesses burn the code (`429`); request a new one.
- Unverified sign-ups are **deleted 1 hour** after their last code was sent (`PENDING_REGISTRATION_TTL`),
  so the email can be registered again from scratch.
- Until the code is verified there is **no account**: login, forgot-password, tokens: nothing works.

### Exact error messages (auth)

| Situation | Status | `message` |
|---|---|---|
| Bad email format (`abc@gmail`, `a b@x.com`) | 400 | `email must be a valid email address, e.g. name@example.com` |
| Several invalid fields | 400 | all of them, e.g. `fullName must not be blank; password size must be between 8 and 72` |
| Email domain can't receive mail (`gmail.con`) | 400 | `This email address cannot receive mail. Check it for typos` |
| Body missing / broken JSON | 400 | `Request body is missing or is not valid JSON` |
| Register an email that already has an account | 409 | `Email is already registered` |
| New code asked within 60 s | 429 | `Please wait N seconds before requesting a new code` |
| Wrong OTP | 400 | `Incorrect code. N attempts left` |
| 5th wrong OTP | 429 | `Too many wrong attempts. This code is blocked, request a new code` |
| OTP older than 10 min | 400 | `The code has expired. Request a new code` |
| Verify/resend for an email with no sign-up (or > 1 h old) | 404 | `No pending registration for this email (it may have expired). Please register again` |
| Verify/resend for an already verified email | 409 | `This email is already verified. Please log in` |
| Login / forgot-password, unknown email | 404 | `No account found with this email` |
| Login with wrong password | 401 | `Incorrect password` |
| Login before verifying (right password) | 403 | `Email is not verified. Enter the code sent to your email to finish registration` (a new code is emailed) |
| Forgot-password before verifying | 403 | `Email is not verified. Finish registration with the code sent to your email first` |
| Reset password: no active reset code | 400 | `No active code for this email. Request a new code` |
| Reset token wrong or already used | 400 | `Invalid or already used reset token. Verify the reset code again` |
| Reset token older than 15 min | 400 | `The reset token has expired. Verify the reset code again` |
| Email server failed | 503 | `Could not send email. Please try again later` |
- No email set up? With `MAIL_ENABLED=false` the code is printed in the server log (line `MAIL DISABLED ...`).

### 4.1 Register 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/register` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "email": "{{email}}", "password": "{{password}}", "fullName": "Alice", "currencyCode": "BDT" }
```
Rules:
- **email**: real format `name@domain.tld` (`a@b`, `a b@x.com`, `x@gmail` → 400), stored lowercase, unique
  case-insensitively, and its **domain must be able to receive mail** (DNS check: `gmail.con`,
  `no-such-domain.com` → `400 This email address cannot receive mail`).
- **password** 8–72 chars · **fullName** ≤ 100 (field name is `fullName`, not `name`) · **currencyCode** 3 uppercase letters.

**No account is created here.** The details wait as a *pending registration*; the account is created
only when the OTP is verified (4.2). A wrong/fake email therefore never leaves an account behind.
Unverified sign-ups are deleted automatically **1 hour** after the last code was sent; then the email can register again.

**201** (no tokens yet; check your email)
```json
{ "message": "Verification code sent to alice@example.com" }
```
Registering again with an email that is **not verified yet** is allowed: it replaces the pending details and
sends a new code (wait 60 s between codes). If the email fails to send, nothing is saved and you get `503`.
Errors: `400` validation / undeliverable domain, `409` email already registered, `429` cooldown, `503` email failed (exact messages in the table above).

👉 Next: copy the code from the email into the collection variable `otp`.

### 4.2 Verify email 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/verify-email` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "email": "{{email}}", "otp": "{{otp}}" }
```
**200**: the account is created now and you are logged in.
```json
{ "accessToken": "eyJhbGciOi...", "refreshToken": "2yQ1b...", "tokenType": "Bearer", "expiresIn": 900 }
```
Post-response script: saves `accessToken` and `refreshToken` (see §1.6).
Errors: `400` incorrect/expired code, `404` no pending sign-up, `409` already verified, `429` too many wrong attempts.

### 4.3 Resend OTP 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/resend-otp` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body (`purpose` optional: `EMAIL_VERIFICATION` (default) or `PASSWORD_RESET`):
```json
{ "email": "{{email}}", "purpose": "EMAIL_VERIFICATION" }
```
**200** `{ "message": "Verification code sent to alice@example.com" }`
(or `"Password reset code sent to ..."` for `PASSWORD_RESET`).
Errors: `404` no pending sign-up / no account, `409` already verified, `429` inside the 60 s cooldown.

### 4.4 Login 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/login` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "email": "{{email}}", "password": "{{password}}" }
```
**200**: same body as verify-email. Post-response script saves both tokens.
Errors: `404 No account found with this email`, `401 Incorrect password`, `403 EMAIL_NOT_VERIFIED` sign-up not verified yet.

**Unverified account** (registered but never entered the code):
- Login with the right password → `403 EMAIL_NOT_VERIFIED` **and a new code is emailed automatically**
  (unless one was sent in the last 60 s, then the old one is still valid). Show the OTP screen → 4.2.
- Registering again with the same email is allowed (`201`): the new name/password/currency replace the
  pending ones and a new code is sent. Only an existing (verified) account gives `409`.
- Wrong password for a pending sign-up → `401 Incorrect password`.
- Not verified within 1 hour → the sign-up is deleted; login then gives `404` and you can register again.

### 4.5 Refresh token 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/refresh` |
| Authorization | No Auth (the refresh token goes in the **body**, not the header) |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "refreshToken": "{{refreshToken}}" }
```
**200**: new pair (same shape as login). Post-response script saves both. The old refresh token stops working.
`401` if unknown, expired or revoked. Re-using a revoked token revokes **all** the user's sessions.

### 4.6 Logout 🔒
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/logout` |
| Authorization | Bearer Token `{{accessToken}}` |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "refreshToken": "{{refreshToken}}" }
```
**200** `message: "Logged out successfully"` (no `data`). Idempotent. `401` without a valid access token.

### 4.7 Forgot password 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/forgot-password` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "email": "{{email}}" }
```
**200** `{ "message": "Password reset code sent to alice@example.com" }`
Errors: `404 No account found with this email`, `403` email not verified yet, `429` cooldown.
👉 Put the emailed code in `otp`.

### 4.8 Verify reset OTP 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/verify-reset-otp` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body:
```json
{ "email": "{{email}}", "otp": "{{otp}}" }
```
**200**
```json
{ "resetToken": "XSuhizgiluIDBXyXjsMROtp9wmCokbyw93pxpA8Wer8", "expiresIn": 900 }
```
Post-response script: `pm.collectionVariables.set("resetToken", pm.response.json().data.resetToken);`
Single use, valid 15 minutes. Errors: `404` no account, `400` incorrect/expired/no active code, `429` too many attempts.

### 4.9 Reset password 🔓
| | |
|---|---|
| Method / URL | `POST {{baseUrl}}/auth/reset-password` |
| Authorization | No Auth |
| Headers | `Content-Type: application/json` |

Body (newPassword 8–72 chars):
```json
{ "resetToken": "{{resetToken}}", "newPassword": "newpassword1" }
```
**200** `message: "Password reset successfully. Please log in with your new password"`. Also marks the email verified and **revokes all refresh tokens** (signs out every device);
log in again with the new password. `400` invalid, already used or expired token (exact message tells which).

---

## 5. User 🔒

All requests: **Authorization → Bearer Token `{{accessToken}}`** (inherit from collection).

### 5.1 Get my profile
`GET {{baseUrl}}/users/me` · no body

**200**
```json
{ "id": "6851ddfe-...", "email": "alice@example.com", "fullName": "Alice", "currencyCode": "BDT",
  "createdAt": "2026-11-25T04:25:50.837Z" }
```

### 5.2 Update my profile
`PUT {{baseUrl}}/users/me` · Header `Content-Type: application/json`
```json
{ "fullName": "Alice R", "currencyCode": "USD" }
```
**200**: updated profile (same shape as 5.1).

### 5.3 Change password (logged in)
`PUT {{baseUrl}}/users/me/password` · Header `Content-Type: application/json`
```json
{ "currentPassword": "password123", "newPassword": "newpassword1" }
```
**200** `message: "Password changed successfully..."`. `400` if the current password is wrong. All refresh tokens are revoked; the current
access token keeps working until it expires. Log in again to get a new pair.

---

## 6. Categories 🔒

All requests: **Bearer `{{accessToken}}`**.

### 6.1 List
`GET {{baseUrl}}/categories?type=EXPENSE` · no body · `type` optional (`EXPENSE` / `INCOME`)

**200**: system defaults + your own:
```json
[
  { "id": "2678c9a0-...", "name": "Food", "type": "EXPENSE", "icon": "restaurant", "color": "#F44336", "systemDefault": true },
  { "id": "9b1e...",      "name": "Pets", "type": "EXPENSE", "icon": "pets",       "color": "#FF9800", "systemDefault": false }
]
```
Tip: copy a default category `id` here for creating transactions (the collection saves `foodCategoryId` and `salaryCategoryId` automatically).

### 6.2 Create / 6.3 Update
`POST {{baseUrl}}/categories` → **201** · `PUT {{baseUrl}}/categories/{{categoryId}}` → **200**
Header `Content-Type: application/json`
```json
{ "name": "Pets", "type": "EXPENSE", "icon": "pets", "color": "#FF9800" }
```
Response: the category (shape as in 6.1). Create's script saves `categoryId`.

### 6.4 Delete
`DELETE {{baseUrl}}/categories/{{categoryId}}` · no body → **200** + `message` (no `data`)

| Case | Result |
|---|---|
| name already used by a default or own category of the same type (case-insensitive) | 409 |
| editing/deleting a system default | 403 |
| another user's category | 404 |
| changing `type` while transactions use it | 409 |
| deleting while used by transactions or budgets | 409 |

---

## 7. Transactions 🔒

All requests: **Bearer `{{accessToken}}`**.

### 7.1 Create / 7.4 Update
`POST {{baseUrl}}/transactions` → **201** · `PUT {{baseUrl}}/transactions/{{transactionId}}` → **200**
Header `Content-Type: application/json`
```json
{ "type": "EXPENSE", "amount": 250.50, "categoryId": "{{foodCategoryId}}", "transactionDate": "2026-11-05", "note": "Lunch" }
```
Rules: `amount > 0` with max 2 decimals; `type` must equal the category's type (else 400);
category must be a default or your own (else 404); note ≤ 255 (optional).

**201**: create's script saves `transactionId`.
```json
{ "id": "f1c2...", "type": "EXPENSE", "amount": 250.50, "categoryId": "2678c9a0-...", "categoryName": "Food",
  "note": "Lunch", "transactionDate": "2026-11-05", "createdAt": "2026-11-25T04:30:00Z" }
```

### 7.2 Get one
`GET {{baseUrl}}/transactions/{{transactionId}}` · no body → **200** (same shape). `404` if not yours.

### 7.3 List
`GET {{baseUrl}}/transactions?type=EXPENSE&from=2026-11-01&to=2026-11-30&page=0&size=20&sort=amount,desc` · no body

In Postman you can fill the **Params** tab instead of typing the query string. All params are optional:

| Param | Example | Meaning |
|---|---|---|
| `type` | `EXPENSE` | filter by type |
| `categoryId` | uuid | filter by category |
| `from`, `to` | `2026-11-01` | inclusive date range |
| `minAmount`, `maxAmount` | `10`, `500` | inclusive amount range |
| `q` | `lunch` | case-insensitive text search in note |
| `page`, `size`, `sort` | `0`, `20`, `amount,asc` | see Pagination |

**200**: page object (see §2). Default sort: `transactionDate DESC, createdAt DESC`.
Sortable fields: `transactionDate`, `amount`, `createdAt`, `type`.
`400` for unknown sort field, `from > to`, `minAmount > maxAmount`, or malformed values.

### 7.5 Delete
`DELETE {{baseUrl}}/transactions/{{transactionId}}` · no body → **200** + `message` (no `data`)

---

## 8. Budgets 🔒

All requests: **Bearer `{{accessToken}}`**.

### 8.1 Create / 8.3 Update
`POST {{baseUrl}}/budgets` → **201** · `PUT {{baseUrl}}/budgets/{{budgetId}}` → **200**
Header `Content-Type: application/json`
```json
{ "categoryId": null, "amount": 30000, "year": {{year}}, "month": {{month}} }
```
`categoryId: null` = overall budget (all expenses). Otherwise use an **EXPENSE** category id,
e.g. `"categoryId": "{{foodCategoryId}}"`. One budget per (category, year, month) → `409` on duplicates.
`month` 1–12, `year` 2000–2100. Create's script saves `budgetId`.

### 8.2 List for a month
`GET {{baseUrl}}/budgets?year={{year}}&month={{month}}` · no body

**200**
```json
[
  { "id": "e83a...", "categoryId": null, "categoryName": null, "amount": 30000.00, "spent": 380.00, "remaining": 29620.00, "year": 2026, "month": 11 },
  { "id": "66a1...", "categoryId": "2678...", "categoryName": "Food", "amount": 5000.00, "spent": 300.00, "remaining": 4700.00, "year": 2026, "month": 11 }
]
```
`spent` = sum of EXPENSE transactions in that calendar month (overall = all categories).
`remaining` can be negative when over budget. Overall budget is listed first.

### 8.4 Delete
`DELETE {{baseUrl}}/budgets/{{budgetId}}` · no body → **200** + `message` (no `data`)

---

## 9. Reports 🔒

All requests: **Bearer `{{accessToken}}`**, `GET`, no body. Ranges are inclusive; only your data is included.

### 9.1 Summary
`GET {{baseUrl}}/reports/summary?from=2026-01-01&to=2026-12-31`
```json
{ "from": "2026-01-01", "to": "2026-12-31", "totalIncome": 50000.00, "totalExpense": 380.00, "balance": 49620.00 }
```

### 9.2 Expense by category
`GET {{baseUrl}}/reports/by-category?from=2026-01-01&to=2026-12-31`
```json
[
  { "categoryId": "...", "categoryName": "Rent", "amount": 1000.00, "percentage": 66.67 },
  { "categoryId": "...", "categoryName": "Food", "amount": 500.00,  "percentage": 33.33 }
]
```
Expenses only, sorted by amount descending.

### 9.3 Monthly trend
`GET {{baseUrl}}/reports/monthly?year={{year}}`
```json
[ { "month": 1, "income": 5000.00, "expense": 1250.25 }, { "month": 2, "income": 0.00, "expense": 0.00 }, ... ]
```
Always 12 entries.

---

## 10. Health 🔓

`GET http://localhost:8080/actuator/health` (not under `/api/v1`) · No Auth → **200** `{"status":"UP"}`

---

## 11. Common Postman mistakes

| You see | Cause | Fix |
|---|---|---|
| `401 UNAUTHORIZED` on a protected endpoint | no token, or it expired (15 min) | Authorization tab = Bearer `{{accessToken}}` / inherit; run **Refresh token** or **Login** |
| `401` even with a token | you typed `Bearer` inside the Bearer Token field | the field takes only the token: `{{accessToken}}` |
| `403 EMAIL_NOT_VERIFIED` on login | email not verified | run Verify Email with the code (or Resend OTP) |
| `400 Malformed JSON` | Body not set to raw → JSON, or a trailing comma | Body → raw → JSON, check the JSON |
| `415 Unsupported Media Type` | missing `Content-Type: application/json` | choose raw → **JSON** (adds the header) |
| `400 Incorrect code` / `The code has expired` | wrong `{{otp}}`, an older code, or > 10 min | Resend OTP, use the newest code |
| `404 No pending registration ...` | sign-up not verified within 1 hour, so it was deleted | Register again |
| No OTP email arrives | see `docs/SETUP.md` → *OTP email not arriving* | check the `Mail:` line in the server log |
| `429` | asked for a new code within 60 s, or 5 wrong codes | wait, then Resend OTP |
| `{{variable}}` shown red / sent literally | variable not set or wrong environment | select the environment; check collection *Variables* |
| `Could not send request` | server not running | `./scripts/dev.sh` |
| `400` with path `/api/v1//auth/...` (double `//`) | `baseUrl` ends with `/` | baseUrl must be `http://localhost:<PORT>/api/v1` with **no** `/` at the end |
| Different port (e.g. 8000) | server runs on `PORT` from `.env` (default 8080) | set `PORT=8000` in `.env`, restart, and set `baseUrl` = `http://localhost:8000/api/v1` |
