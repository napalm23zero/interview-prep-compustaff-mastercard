<div align="center">

# 💳 Payment Eligibility Service

### A self-directed training challenge — Spring Boot + React

_Build a payment eligibility API and UI in 45 minutes. Handle the error cases like production depends on it._

<br>

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1-6DB33F?style=flat-square&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-3.9-C71A36?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![H2](https://img.shields.io/badge/H2-in--memory-1021FF?style=flat-square&logo=databricks&logoColor=white)](https://www.h2database.com/)
[![React](https://img.shields.io/badge/React-19-61DAFB?style=flat-square&logo=react&logoColor=black)](https://react.dev/)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0-3178C6?style=flat-square&logo=typescript&logoColor=white)](https://www.typescriptlang.org/)
[![Vite](https://img.shields.io/badge/Vite-8-646CFF?style=flat-square&logo=vite&logoColor=white)](https://vite.dev/)
[![Node](https://img.shields.io/badge/Node-20-5FA04E?style=flat-square&logo=nodedotjs&logoColor=white)](https://nodejs.org/)

![Tests](https://img.shields.io/badge/backend_tests-41_passing-success?style=flat-square&logo=junit5&logoColor=white)
![Tasks](https://img.shields.io/badge/tasks_1--3-complete-success?style=flat-square)
![Frontend](https://img.shields.io/badge/task_4-in_progress-yellow?style=flat-square)
![Time budget](https://img.shields.io/badge/time_budget-45_min-blue?style=flat-square&logo=clockify&logoColor=white)
![Devcontainer](https://img.shields.io/badge/devcontainer-ready-2496ED?style=flat-square&logo=docker&logoColor=white)

</div>

---

## ⚠️ Disclaimer — please read first

> This repository is a **personal, self-designed training exercise**. It is **not** an
> assessment, and it was **not** requested, commissioned, reviewed, or endorsed by
> **CompuStaff**, **Mastercard**, **Ropes.ai**, or any other company.
>
> - The challenge below was **written by me**, from scratch, as a practice scenario.
> - It contains **no** material from any real assessment. No proprietary assessment
>   content was accessed, copied, reproduced, or reverse-engineered.
> - It is **not** a leak, a dump, or a reconstruction of anyone's hiring test.
> - Nothing here will be submitted to anyone. It exists purely so I can practise.
> - Company and product names appear only to describe **my own preparation context**.
>   All trademarks belong to their respective owners. No affiliation is claimed or implied.
>
> Think of it as a kata I wrote for myself — like solving a self-invented problem before
> a driving test, except nobody handed me the exam.

---

## 🎯 Why this exists

I received an invitation to a timed technical assessment: **45 minutes**, Java / Spring
Boot / React, on the theme of a payment eligibility service. Rather than walk in cold, I
wrote my own version of a problem in that shape and built it end to end.

The goal is not this code. The goal is that on test day the reflexes are automatic:

| Reflex | Why it matters |
| --- | --- |
| 🧪 Run the provided tests **before** writing a line | The tests are the real specification |
| 💰 `BigDecimal` from `String`, compared with `compareTo` | `0.1 + 0.2 != 0.3` — money is never a `double` |
| ✅ Business rejection is **200**, not 4xx | "No, and here's why" is a successful answer |
| 🧾 One consistent error envelope, no stack traces | Error handling is half the grade, not the leftovers |
| 🌐 `fetch` resolves on 400 — check `res.ok` | The single most common frontend bug in take-homes |
| ✂️ Write what the task asks, not what production asks | An interface with one implementation is 20 wasted minutes |

Progress is meant to be **incremental and readable**: small steps, honest test names,
decisions I can defend out loud.

---

## 🚀 Quick start

Everything runs inside the devcontainer (Debian 13, JDK 21, Maven 3.9, Node 20).

```bash
# backend — http://localhost:8080
cd app/backend && ./mvnw spring-boot:run

# frontend — http://localhost:5173
cd app/frontend && npm install && npm run dev

# the test suite
cd app/backend && ./mvnw test
```

| What | Where |
| --- | --- |
| 📘 Swagger UI | http://localhost:8080/swagger-ui.html |
| 📄 OpenAPI JSON | http://localhost:8080/v3/api-docs |
| 🗄️ H2 console | http://localhost:8080/h2-console — `jdbc:h2:mem:compustaff` · `sa` · _(no password)_ |

VS Code users: <kbd>F5</kbd> → **full stack: backend + frontend** brings both up at once,
freeing port 8080 first.

---

## 📐 Architecture

Feature-first packages. Each business module owns its `domain` (concepts), `dto`
(boundary), `service` / `usecase` (behaviour) and `repository` (persistence).

```
app/backend/src/main/java/dev/hustletech/interview/compustaff/
├── account/       domain · dto · repository · service · usecase
├── document/      domain · dto · repository
├── kyc/           domain · dto · repository · service · usecase
├── eligibility/   domain · dto · controller · usecase      ← the core of the challenge
└── shared/        config (OpenAPI) · exception (envelope + handler)

app/frontend/src/
└── api/           types.ts (the contract) · client.ts       ← UI is built on top of these
```

`eligibility` has no repository on purpose: a decision is computed from `account` and
`kyc`, never stored.

---

<div align="center">

# 📋 The Challenge

_Everything below is the exercise brief, written as if it had been handed to me._

</div>

---

## Scenario

You are working on the payments platform for a card issuer. Before a payment is submitted
for processing, an **eligibility check** determines whether the account is allowed to make
that payment. The check does **not** move money — it returns a decision and the reasons
behind it.

The backend service and the React UI are already scaffolded and running. Several pieces are
incomplete: they are marked with `TODO` in the source and covered by failing tests.

## Time

**45 minutes.** The scope is intentionally larger than what most candidates finish.
Prioritise: a smaller amount of correct, well-handled work scores higher than a larger
amount of fragile work.

## Stack (already set up — do not change)

| Layer    | Technology                    |
| -------- | ----------------------------- |
| Backend  | Java 21, Spring Boot 3, Maven |
| Frontend | React 19, TypeScript, Vite    |
| Storage  | In-memory repository          |
| Tests    | JUnit 5, Spring MockMvc       |

## Getting started

```bash
# backend — starts on :8080
cd backend && mvn spring-boot:run

# frontend — starts on :5173
cd frontend && npm install && npm run dev

# run the test suite (several tests fail on a fresh checkout — that is expected)
cd backend && mvn test
```

`backend/` holds the domain, the repository and the web layer. `frontend/src/` holds the
HTTP client, the form and the result panel.

**Read the failing tests before writing any code.** They define the expected behaviour more
precisely than this document does. Where this document is silent, the tests are the
authority; where both are silent, the decision is yours to make and to justify.

## The data model

```java
public record Account(
    String id,
    String holderName,
    AccountStatus status,      // ACTIVE, SUSPENDED, CLOSED
    boolean kycVerified,
    String currency,           // ISO-4217, e.g. "USD"
    BigDecimal balance,
    BigDecimal heldAmount,     // funds reserved by pending transactions
    BigDecimal dailyLimit,     // nullable — null means no daily limit
    BigDecimal dailySpent,
    BigDecimal minTransaction,
    BigDecimal maxTransaction  // nullable — null means no per-transaction ceiling
) {
    public BigDecimal availableBalance() {
        return balance.subtract(heldAmount);
    }
}
```

`dailyLimit` and `maxTransaction` are the **only** nullable fields. A null value means
_unlimited_, not _zero_. `availableBalance()` is already implemented — `balance` alone is
not the amount an account can spend.

---

## Task 1 — Core eligibility logic

Implement the evaluation. A request is eligible only if **none** of these rules is
violated:

| #   | Reason code            | Rejected when                                  |
| --- | ---------------------- | ---------------------------------------------- |
| 1   | `ACCOUNT_INACTIVE`     | `status != ACTIVE`                             |
| 2   | `KYC_NOT_VERIFIED`     | `kycVerified` is false                         |
| 3   | `CURRENCY_MISMATCH`    | request currency differs from account currency |
| 4   | `AMOUNT_BELOW_MINIMUM` | `amount < minTransaction`                      |
| 5   | `AMOUNT_ABOVE_MAXIMUM` | `amount > maxTransaction`                      |
| 6   | `INSUFFICIENT_FUNDS`   | `amount > availableBalance()`                  |
| 7   | `DAILY_LIMIT_EXCEEDED` | `dailySpent + amount > dailyLimit`             |

Requirements:

- Return **every** applicable reason, not just the first. Reason order must be
  deterministic and follow the declaration order of `RejectionReason`.
- **Exception — a `CLOSED` account is terminal.** When `status == CLOSED`, the only reason
  returned is `ACCOUNT_INACTIVE`, even if other rules are also violated. A closed account
  is not a payment that can be fixed; reporting the other reasons would imply it could be.
  `SUSPENDED` is **not** terminal: it accumulates with every other applicable reason.
- Rules 5 and 7 do not apply when their limit is null.
- All monetary comparisons use `BigDecimal.compareTo`. Never `equals`, never `double`.
- If the account does not exist, throw `AccountNotFoundException` (already provided). It is
  not a rejection reason.

## Task 2 — API behaviour

### `POST /api/payments/eligibility`

Request:

```json
{ "accountId": "ACC-1001", "amount": "250.5", "currency": "USD" }
```

Response — `200 OK` in **both** the approved and rejected cases:

```json
{
  "accountId": "ACC-1008",
  "eligible": false,
  "decision": "REJECTED",
  "reasons": ["KYC_NOT_VERIFIED", "INSUFFICIENT_FUNDS", "DAILY_LIMIT_EXCEEDED"],
  "amount": "250.50",
  "availableBalance": "50.00",
  "evaluatedAt": "2026-08-27T14:22:05Z"
}
```

A rejection is a **successful answer to the question asked**, not a protocol error. Only
malformed or unprocessable input produces a 4xx.

### `GET /api/accounts` · `GET /api/accounts/{id}`

All accounts ordered by `id` ascending, and a single account or `404`.

### Monetary values

Every monetary field in a response is a **JSON string** carrying exactly two decimal
places, `HALF_UP`. `"250.5"` in, `"250.50"` out. JSON numbers are IEEE-754 doubles; the
browser would lose precision on parse.

Note that normalising the scale for output must not change the _comparisons_ the rules
perform.

### Validation

| Field       | Constraint                                                     |
| ----------- | -------------------------------------------------------------- |
| `accountId` | not blank                                                      |
| `amount`    | not null, strictly greater than zero, at most 2 decimal places |
| `currency`  | not null, exactly 3 uppercase letters                          |

## Task 3 — Error handling

Every error response uses this single envelope:

```json
{
  "timestamp": "2026-08-27T14:22:05Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [{ "field": "amount", "message": "must be greater than 0" }]
}
```

`fieldErrors` is present only for `VALIDATION_ERROR`.

| Condition                   | Status | `code`              |
| --------------------------- | ------ | ------------------- |
| Bean Validation failure     | `400`  | `VALIDATION_ERROR`  |
| Malformed JSON / wrong type | `400`  | `MALFORMED_REQUEST` |
| Unknown account             | `404`  | `ACCOUNT_NOT_FOUND` |
| Anything else               | `500`  | `INTERNAL_ERROR`    |

No unhandled exception may reach the client as a raw stack trace. A null limit must never
surface as a `500`.

## Task 4 — Form interactions

A controlled form with `accountId`, `amount` and `currency`, calling the API and showing
the outcome. What must be true when you are done:

- The form is never in two states at once, and never stuck in a loading state — including
  when the backend is unreachable.
- The submit button cannot trigger a second in-flight request.
- A non-2xx response is never treated as success. Field errors from a `400` appear next to
  the input they belong to, not in a generic banner.
- A network failure is distinguishable, in the UI, from a rejected payment.
- Rejection codes are never shown raw. `KYC_NOT_VERIFIED` reads as "KYC verification is
  pending", and so on for all seven.
- **Out-of-order responses cannot corrupt the screen.** If two submissions overlap, the
  result displayed must belong to the most recent one — never to whichever response happens
  to arrive last.

## Task 5 — Idempotent evaluation _(stretch — only after Tasks 1–4 pass)_

`POST /api/payments/eligibility` accepts an optional `Idempotency-Key` header.

- Absent header: current behaviour, unchanged.
- Key seen before with an **identical** request body: return the stored response verbatim,
  without re-evaluating. `evaluatedAt` must not change.
- Key seen before with a **different** request body: `409` with code
  `IDEMPOTENCY_KEY_REUSED`, using the standard error envelope.

---

## Seed accounts

`—` means null.

```
id         status      kyc  ccy  balance     held     dailyLimit  dailySpent  min    max
ACC-1001   ACTIVE      yes  USD     5000.00     0.00    10000.00        0.00   1.00   2500.00
ACC-1002   ACTIVE      yes  USD      100.00     0.00    10000.00        0.00   1.00   2500.00
ACC-1003   SUSPENDED   yes  USD     5000.00     0.00    10000.00        0.00   1.00   2500.00
ACC-1004   ACTIVE      no   USD     5000.00     0.00    10000.00        0.00   1.00   2500.00
ACC-1005   ACTIVE      yes  EUR     5000.00     0.00    10000.00        0.00   1.00   2500.00
ACC-1006   ACTIVE      yes  USD     5000.00     0.00     1000.00      900.00   1.00   2500.00
ACC-1007   ACTIVE      yes  USD     5000.00  4900.00    10000.00        0.00   1.00   2500.00
ACC-1008   ACTIVE      no   USD       50.00     0.00     1000.00      990.00   1.00   2500.00
ACC-1009   CLOSED      no   EUR       10.00     0.00      100.00       99.00   1.00   2500.00
ACC-1010   SUSPENDED   no   USD     5000.00     0.00    10000.00        0.00   1.00   2500.00
ACC-1011   ACTIVE      yes  USD  1000000.00     0.00           —        0.00   1.00         —
```

## Acceptance criteria

The provided suite covers these. It is not exhaustive — hidden tests check the same
behaviours with different inputs.

**Eligibility**

- [x] `ACC-1001` + `250.00 USD` → eligible, empty `reasons`
- [x] `ACC-1002` + `250.00 USD` → `["INSUFFICIENT_FUNDS"]`
- [x] `ACC-1005` + `250.00 USD` → `["CURRENCY_MISMATCH"]`
- [x] `ACC-1007` + `250.00 USD` → `["INSUFFICIENT_FUNDS"]` (available is 100.00, not 5000.00)
- [x] `ACC-1008` + `250.00 USD` → exactly
      `["KYC_NOT_VERIFIED", "INSUFFICIENT_FUNDS", "DAILY_LIMIT_EXCEEDED"]`, in that order
- [x] `ACC-1010` + `250.00 USD` → `["ACCOUNT_INACTIVE", "KYC_NOT_VERIFIED"]` — suspended accumulates
- [x] `ACC-1009` + `250.00 USD` → exactly `["ACCOUNT_INACTIVE"]` — closed is terminal,
      even though KYC, currency and funds all fail too
- [x] `ACC-1011` + `999999.99 USD` → eligible — null limits are unlimited, not zero
- [x] `ACC-1001` + `2500.00 USD` → eligible (boundary: `<=` max, not `<`)
- [x] `ACC-1001` + `1.00 USD` → eligible (boundary: `>=` min)
- [x] `ACC-1006` + `100.00 USD` → eligible (boundary: `900 + 100 == 1000`, not over)
- [x] `ACC-1001` + `1.0 USD` → eligible, and the response echoes `"1.00"`

**API**

- [x] Rejected evaluation returns `200`, not `422` or `400`
- [x] `amount` of `"0.00"` or `"-5.00"` → `400 VALIDATION_ERROR`
- [x] `amount` of `"250.500"` → `400 VALIDATION_ERROR`; `"250.5"` is accepted
- [x] `currency` of `"usd"` or `"US"` → `400 VALIDATION_ERROR`, not `CURRENCY_MISMATCH`
- [x] Unknown account → `404 ACCOUNT_NOT_FOUND` with the standard envelope
- [x] `{"amount": "abc"}` → `400 MALFORMED_REQUEST`, never `500`
- [x] `ACC-1011` never produces a `500` from its null limits
- [x] Monetary fields are JSON strings with two decimal places

**Frontend**

- [ ] Submit is disabled while a request is in flight
- [ ] Field errors appear next to their input
- [ ] Stopping the backend and submitting shows an error, and loading ends
- [ ] Reason codes are never displayed raw
- [ ] Two overlapping submissions leave the newer result on screen

## Deliberate ambiguity

One decision is **not** specified anywhere, and no test pins it down:

> When `CURRENCY_MISMATCH` applies, the requested amount is denominated in a different
> currency from the account. Should the amount-based rules — minimum, maximum, funds,
> daily limit — still be evaluated against it, or is comparing across currencies
> meaningless?

Decide. Implement it consistently. Leave a one-line comment stating which way you went and
why. A defended decision scores; an accidental one does not.

## How you're scored

| Category                  | Weight | What it looks at                                                             |
| ------------------------- | ------ | ---------------------------------------------------------------------------- |
| Core logic correctness    | 30%    | rules, boundaries, null limits, terminal status, `BigDecimal` handling       |
| API behaviour             | 25%    | status codes, contract adherence, validation at the boundary                 |
| Error handling            | 25%    | consistent envelope, no leaked stack traces, frontend resilience             |
| Frontend interaction      | 10%    | state handling, disabled controls, ordering, readable output                 |
| Judgement & communication | 10%    | naming, layering, test names, incremental progress, decisions you can defend |

Notes on how this is assessed:

- Your work is reviewed as prose as well as code. Clear names and obvious structure score
  higher than clever one-liners that happen to be correct.
- Progress is reviewed as a timeline. Running the tests first and building incrementally
  reads better than a single large change at the end.
- Finishing four tasks well beats starting five.

## Notes

- Do not restructure the project or introduce new dependencies.
- Do not add an interface with a single implementation "for flexibility".
- If you run out of time, leave a short `TODO` stating what you would do next and why. A
  stated plan scores better than silence.

---

<div align="center">

### 📚 Working documents

[**AGENTS.md**](AGENTS.md) — how AI agents should work in this repo &nbsp;·&nbsp;
[**CHAT.md**](CHAT.md) — decisions, lessons, open questions &nbsp;·&nbsp;
[**GUIDE.md**](GUIDE.md) — architecture and execution plan

<br>

_Built to practise, not to ship._ 🧪

</div>
