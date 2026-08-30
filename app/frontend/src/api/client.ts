import type { EligibilityRequest, EligibilityResponse, ErrorResponse } from "./types";

/**
 * Everything a call can come back as. The caller never sees an HTTP status, and never has
 * to tell an exception from an answer.
 */
export type CheckResult =
  | { kind: "decided"; response: EligibilityResponse }
  | { kind: "invalid"; fieldErrors: Record<string, string> }
  | { kind: "failed"; message: string }
  | { kind: "aborted" };

export async function checkEligibility(
  request: EligibilityRequest,
  signal?: AbortSignal,
): Promise<CheckResult> {
  let response: Response;

  try {
    response = await fetch("/api/payments/eligibility", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request),
      signal,
    });
  } catch {
    // Aborted is not a failure: the caller replaced this request with a newer one.
    if (signal?.aborted) return { kind: "aborted" };

    return { kind: "failed", message: "Could not reach the eligibility service." };
  }

  // fetch only rejects on a network failure, so a 400 arrives here as a resolved promise.
  // res.ok is the single line that keeps a rejected request from reading as a decision.
  if (response.ok) {
    return { kind: "decided", response: (await response.json()) as EligibilityResponse };
  }

  let envelope: ErrorResponse;

  try {
    envelope = (await response.json()) as ErrorResponse;
  } catch {
    // A non-2xx that is not our envelope: something between us and the API broke.
    return { kind: "failed", message: "The eligibility service is not responding correctly." };
  }

  if (envelope.code === "VALIDATION_ERROR" && envelope.fieldErrors?.length) {
    return {
      kind: "invalid",
      fieldErrors: Object.fromEntries(envelope.fieldErrors.map((e) => [e.field, e.message])),
    };
  }

  // An unknown account is a problem with what was typed, so it belongs to that field.
  if (envelope.code === "ACCOUNT_NOT_FOUND") {
    return { kind: "invalid", fieldErrors: { accountId: "No account with this id" } };
  }

  return { kind: "failed", message: envelope.message };
}
