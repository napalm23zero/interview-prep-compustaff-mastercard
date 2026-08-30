import { useRef, useState, type FormEvent } from "react";

import { checkEligibility, type CheckResult } from "./api/client";
import type { RejectionReason } from "./api/types";

/** A reason code is a wire value. Nobody outside the API should ever read one. */
const REASON_LABELS: Record<RejectionReason, string> = {
  ACCOUNT_INACTIVE: "This account is not active",
  KYC_NOT_VERIFIED: "KYC verification is pending",
  CURRENCY_MISMATCH: "The account does not hold this currency",
  AMOUNT_BELOW_MINIMUM: "Amount is below the minimum for this account",
  AMOUNT_ABOVE_MAXIMUM: "Amount is above the per-transaction limit",
  INSUFFICIENT_FUNDS: "Available balance is not enough",
  DAILY_LIMIT_EXCEEDED: "This would exceed the daily limit",
};

/**
 * One value for the whole screen: the form cannot be loading and showing a result at the
 * same time, because there is nowhere to store both.
 */
type Outcome = { kind: "idle" } | { kind: "loading" } | CheckResult;

export default function App() {
  const [accountId, setAccountId] = useState("ACC-1001");
  const [amount, setAmount] = useState("250.00");
  const [currency, setCurrency] = useState("USD");
  const [outcome, setOutcome] = useState<Outcome>({ kind: "idle" });

  const inFlight = useRef<AbortController | null>(null);

  async function submit(event: FormEvent) {
    event.preventDefault();

    // Whatever is still in flight belongs to an older submission. Cancelling it is what
    // puts the newest result on screen rather than the last one to arrive.
    inFlight.current?.abort();
    const controller = new AbortController();
    inFlight.current = controller;

    setOutcome({ kind: "loading" });

    const result = await checkEligibility({ accountId, amount, currency }, controller.signal);

    // Superseded by a newer submission, which already owns the screen.
    if (result.kind === "aborted") return;

    setOutcome(result);
  }

  const loading = outcome.kind === "loading";
  const errors = outcome.kind === "invalid" ? outcome.fieldErrors : {};

  return (
    <main>
      <h1>Payment eligibility</h1>

      <form onSubmit={submit} noValidate>
        <Field name="accountId" label="Account" value={accountId} onChange={setAccountId} error={errors.accountId} />
        <Field name="amount" label="Amount" value={amount} onChange={setAmount} error={errors.amount} />
        <Field name="currency" label="Currency" value={currency} onChange={setCurrency} error={errors.currency} />

        {/* Disabled in flight: the button cannot start a second request. */}
        <button type="submit" disabled={loading}>
          {loading ? "Checking…" : "Check eligibility"}
        </button>
      </form>

      <Result outcome={outcome} />
    </main>
  );
}

function Field({ name, label, value, error, onChange }: {
  name: string;
  label: string;
  value: string;
  error?: string;
  onChange: (value: string) => void;
}) {
  return (
    <p className="field">
      <label htmlFor={name}>{label}</label>
      <input
        id={name}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        aria-invalid={Boolean(error)}
      />
      {/* Next to the input it belongs to, never in a banner at the top. */}
      {error && <span className="error">{error}</span>}
    </p>
  );
}

function Result({ outcome }: { outcome: Outcome }) {
  if (outcome.kind === "loading") return <p role="status">Checking…</p>;

  // A service that cannot be reached is not a rejected payment, and never reads like one:
  // different panel, different colour, and an alert rather than a status.
  if (outcome.kind === "failed") {
    return (
      <p className="panel failed" role="alert">
        {outcome.message}
      </p>
    );
  }

  // idle, and invalid, whose messages render inside the fields.
  if (outcome.kind !== "decided") return null;

  const { eligible, reasons, amount, availableBalance } = outcome.response;

  return (
    <section className={eligible ? "panel approved" : "panel rejected"} role="status">
      <h2>{eligible ? "Approved" : "Rejected"}</h2>

      <ul>
        {reasons.map((reason) => (
          <li key={reason}>{REASON_LABELS[reason]}</li>
        ))}
      </ul>

      <small>
        {amount} requested · {availableBalance} available
      </small>
    </section>
  );
}
