# Payment eligibility

Decides whether an account may make a payment, and shows the answer in a form. The check
never moves money: it returns a decision and the reasons behind it.

Java 21 · Spring Boot 3.4 · React 19 · TypeScript · Vite · in-memory store

> Personal training exercise, written from scratch. Not an assessment, and containing no
> material from anyone's hiring test.

## Run

```bash
cd app/backend  && ./mvnw spring-boot:run   # http://localhost:8080
cd app/frontend && npm install && npm run dev   # http://localhost:5173
cd app/backend  && ./mvnw test              # 20 tests
```

Vite forwards `/api/*` to `:8080`, so the browser makes same-origin calls and the backend
needs no CORS configuration.

## The endpoint

`POST /api/payments/eligibility`

```json
{ "accountId": "ACC-1001", "amount": "250.5", "currency": "USD" }
```

```json
{
  "eligible": false,
  "reasons": ["ACCOUNT_INACTIVE", "KYC_NOT_VERIFIED", "INSUFFICIENT_FUNDS", "DAILY_LIMIT_EXCEEDED"],
  "amount": "250.50",
  "availableBalance": "50.00"
}
```

Errors use one envelope, with `fieldErrors` present only for `VALIDATION_ERROR`:

```json
{
  "timestamp": "2026-08-27T14:22:05Z",
  "status": 400,
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "fieldErrors": [{ "field": "amount", "message": "must be greater than 0" }]
}
```

Six accounts are seeded in `AccountRepository`, each one built to exercise a different part
of the rules: `ACC-1001` approves and sits exactly on the bounds, `ACC-1002` holds euros,
`ACC-1003` has most of its balance held, `ACC-1004` breaks four rules at once, `ACC-1005`
is closed, `ACC-1006` has no limits at all.

## Decisions worth defending

- **A rejection is `200`.** It is a successful answer to the question asked, not a protocol
  error. Only malformed or unprocessable input produces a 4xx.
- **Monetary values are JSON strings** with two decimal places. JSON numbers are IEEE-754
  doubles and the browser would lose precision parsing them.
- **Scale is applied once, when the response is built**, after every rule has compared the
  exact value the caller sent. Formatting a response cannot change a decision. The response
  type says `String`, so the boundary is visible from the signature.
- **A closed account reports one reason only.** Listing the others would imply the payment
  could be fixed, and it cannot. A suspended one accumulates every applicable reason.
- **Amount rules still run on a currency mismatch.** The caller will fix the currency and
  resubmit, and reporting every problem in one round-trip beats a second rejection for a
  limit we already knew was breached. The one call the spec leaves open.
- **The screen is one discriminated union.** The form cannot be loading and showing a
  result at once, because there is nowhere to store both — and each submission aborts the
  previous one, so what is on screen belongs to the newest request, not the last response
  to arrive.

## Tests

`CheckEligibilityUseCaseTest` covers the rules against the seeded accounts, which are the
fixture: each one exists to violate exactly one rule, so the suite needs no mocking library.
`EligibilityControllerTest` covers the HTTP contract end to end — status codes, monetary
strings and the error envelope.
