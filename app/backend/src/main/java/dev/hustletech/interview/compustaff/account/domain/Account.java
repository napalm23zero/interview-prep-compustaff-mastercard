package dev.hustletech.interview.compustaff.account.domain;

import java.math.BigDecimal;

/** dailyLimit and maxTransaction are the only nullable fields: null means unlimited, never zero. */
public record Account(
        String id,
        AccountStatus status,
        boolean kycVerified,
        String currency,
        BigDecimal balance,
        BigDecimal heldAmount,
        BigDecimal dailyLimit,
        BigDecimal dailySpent,
        BigDecimal minTransaction,
        BigDecimal maxTransaction) {

    /** Balance alone is not spendable: pending transactions hold part of it. */
    public BigDecimal availableBalance() {
        return balance.subtract(heldAmount);
    }

}
