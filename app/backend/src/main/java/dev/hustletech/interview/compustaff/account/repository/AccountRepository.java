package dev.hustletech.interview.compustaff.account.repository;

import static dev.hustletech.interview.compustaff.account.domain.AccountStatus.ACTIVE;
import static dev.hustletech.interview.compustaff.account.domain.AccountStatus.CLOSED;
import static dev.hustletech.interview.compustaff.account.domain.AccountStatus.SUSPENDED;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Repository;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.shared.exception.AccountNotFoundException;

/**
 * In-memory store. The check never writes, so a fixed list is the whole persistence layer.
 * Each account exists to exercise one part of the rules, and doubles as the test fixture.
 *
 * Columns: balance, held, dailyLimit, dailySpent, maxTransaction. minTransaction is always
 * 1.00, and null means unlimited.
 */
@Repository
public class AccountRepository {

    private static final List<Account> ACCOUNTS = List.of(
            // healthy; max and dailyLimit are both 2500.00, so one request proves both bounds
            account("ACC-1001", ACTIVE, true, "USD", "5000.00", "0.00", "2500.00", "0.00", "2500.00"),
            account("ACC-1002", ACTIVE, true, "EUR", "5000.00", "0.00", "10000.00", "0.00", "2500.00"),
            // 4900.00 of the balance is held: only 100.00 is spendable
            account("ACC-1003", ACTIVE, true, "USD", "5000.00", "4900.00", "10000.00", "0.00", "2500.00"),
            // breaks four rules at once
            account("ACC-1004", SUSPENDED, false, "USD", "50.00", "0.00", "1000.00", "990.00", "2500.00"),
            // breaks everything, and may report only ACCOUNT_INACTIVE
            account("ACC-1005", CLOSED, false, "EUR", "10.00", "0.00", "100.00", "99.00", "2500.00"),
            // no daily limit and no per-transaction ceiling: unlimited, not zero
            account("ACC-1006", ACTIVE, true, "USD", "1000000.00", "0.00", null, "0.00", null));

    /** Absence is an error at every call site, so the throw lives here, not in each caller. */
    public Account getById(String id) {
        return ACCOUNTS.stream()
                .filter(account -> account.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new AccountNotFoundException(id));
    }

    private static Account account(String id, AccountStatus status, boolean kycVerified, String currency,
            String balance, String held, String dailyLimit, String dailySpent, String maxTransaction) {

        return new Account(id, status, kycVerified, currency,
                new BigDecimal(balance), new BigDecimal(held), money(dailyLimit),
                new BigDecimal(dailySpent), new BigDecimal("1.00"), money(maxTransaction));
    }

    /** Null stays null: the limit is unlimited, and BigDecimal.ZERO would mean the opposite. */
    private static BigDecimal money(String value) {
        return value == null ? null : new BigDecimal(value);
    }

}
