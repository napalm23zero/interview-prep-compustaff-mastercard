/** The wire contract. Monetary values are strings, never numbers: a JSON number is an
 *  IEEE-754 double and would lose precision on parse. */

export type RejectionReason =
  | "ACCOUNT_INACTIVE"
  | "KYC_NOT_VERIFIED"
  | "CURRENCY_MISMATCH"
  | "AMOUNT_BELOW_MINIMUM"
  | "AMOUNT_ABOVE_MAXIMUM"
  | "INSUFFICIENT_FUNDS"
  | "DAILY_LIMIT_EXCEEDED";

export interface EligibilityRequest {
  accountId: string;
  amount: string;
  currency: string;
}

export interface EligibilityResponse {
  eligible: boolean;
  reasons: RejectionReason[];
  amount: string;
  availableBalance: string;
}

export type ErrorCode =
  | "VALIDATION_ERROR"
  | "MALFORMED_REQUEST"
  | "ACCOUNT_NOT_FOUND"
  | "INTERNAL_ERROR";

export interface FieldError {
  field: string;
  message: string;
}

/** Every error the API returns uses this single envelope. */
export interface ErrorResponse {
  code: ErrorCode;
  message: string;
  /** Present only for VALIDATION_ERROR. */
  fieldErrors?: FieldError[];
}
