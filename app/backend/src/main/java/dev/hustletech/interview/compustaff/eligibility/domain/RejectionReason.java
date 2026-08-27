package dev.hustletech.interview.compustaff.eligibility.domain;

/**
 * Declaration order IS the order reasons appear in the API response.
 * Reordering these constants changes the contract.
 */
public enum RejectionReason {
    ACCOUNT_INACTIVE,
    KYC_NOT_VERIFIED,
    CURRENCY_MISMATCH,
    AMOUNT_BELOW_MINIMUM,
    AMOUNT_ABOVE_MAXIMUM,
    INSUFFICIENT_FUNDS,
    DAILY_LIMIT_EXCEEDED
}
