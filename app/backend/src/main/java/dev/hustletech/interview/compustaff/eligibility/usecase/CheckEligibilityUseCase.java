package dev.hustletech.interview.compustaff.eligibility.usecase;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.account.repository.AccountRepository;
import dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;

@Service
public class CheckEligibilityUseCase {

    private final AccountRepository accountRepository;

    public CheckEligibilityUseCase(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public EligibilityResponseDTO execute(EligibilityRequestDTO request) {
        // Read once, then evaluate every rule against that one snapshot: behind a real
        // store, two reads could disagree halfway through the decision.
        Account account = accountRepository.getById(request.accountId());

        List<RejectionReason> reasons = evaluate(account, request.amount(), request.currency());

        return new EligibilityResponseDTO(reasons.isEmpty(), reasons,
                money(request.amount()), money(account.availableBalance()));
    }

    private List<RejectionReason> evaluate(Account account, BigDecimal amount, String currency) {
        // A CLOSED account is terminal: reporting the other reasons would imply the payment
        // could be fixed, and it cannot.
        if (account.status() == AccountStatus.CLOSED) {
            return List.of(RejectionReason.ACCOUNT_INACTIVE);
        }

        List<RejectionReason> reasons = new ArrayList<>();

        if (account.status() != AccountStatus.ACTIVE) {
            reasons.add(RejectionReason.ACCOUNT_INACTIVE);
        }
        if (!account.kycVerified()) {
            reasons.add(RejectionReason.KYC_NOT_VERIFIED);
        }
        if (!account.currency().equals(currency)) {
            reasons.add(RejectionReason.CURRENCY_MISMATCH);
        }

        // Ambiguity call: the amount rules still run on a currency mismatch. The caller will
        // fix the currency and resubmit, and reporting every problem in one round-trip beats
        // a second rejection for a limit we already knew was breached.
        if (amount.compareTo(account.minTransaction()) < 0) {
            reasons.add(RejectionReason.AMOUNT_BELOW_MINIMUM);
        }
        if (account.maxTransaction() != null && amount.compareTo(account.maxTransaction()) > 0) {
            reasons.add(RejectionReason.AMOUNT_ABOVE_MAXIMUM);
        }
        if (amount.compareTo(account.availableBalance()) > 0) {
            reasons.add(RejectionReason.INSUFFICIENT_FUNDS);
        }
        if (account.dailyLimit() != null
                && account.dailySpent().add(amount).compareTo(account.dailyLimit()) > 0) {
            reasons.add(RejectionReason.DAILY_LIMIT_EXCEEDED);
        }

        return reasons;
    }

    /**
     * The only place scale is touched, and it runs after every rule has already compared the
     * exact value the caller sent. Formatting a response can never change a decision.
     */
    private static String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

}
