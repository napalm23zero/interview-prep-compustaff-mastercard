export type Decision = "APPROVED" | "REJECTED";
export type Reasons =
  | "ACCOUNT_INACTIVE"
  | "KYC_NOT_VERIFIED"
  | "CURRENCY_MISMATCH"
  | "AMOUNT_BELOW_MINIMUM"
  | "AMOUNT_ABOVE_MAXIMUM"
  | "INSUFFICIENT_FUNDS"
  | "DAILY_LIMIT_EXCEEDED";

// request
export interface EligibilityRequest {
  accountId: number;
  amount: string;
  currency: string;
}

// response
export interface EligibilityResponse {
  accountId: number;
  eligible: boolean;
  decision: Decision;
  reasons: Reasons[];
  amount: string;
  availableBalance: string;
  evaluatedAt: string;
}

export type CodeState =
  | "VALIDATION_ERROR"
  | "MALFORMED_REQUEST"
  | "ACCOUNT_NOT_FOUND"
  | "INTERNAL_ERROR";

export interface FieldError {
    field: string;
    message: string;
}

//error
export interface ErrorResponse {
  timestamp: string;
  status: number;
  code: CodecState;
  nessage: string;
  fieldErrors: FieldError[];
}
