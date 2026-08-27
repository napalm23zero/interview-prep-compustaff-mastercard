package dev.hustletech.interview.compustaff.eligibility.usecase.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.account.service.AccountService;
import dev.hustletech.interview.compustaff.eligibility.domain.Decision;
import dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;
import dev.hustletech.interview.compustaff.eligibility.usecase.CheckEligibilityUseCase;
import dev.hustletech.interview.compustaff.kyc.service.KYCService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CheckEligibilityUseCaseImpl implements CheckEligibilityUseCase {

    private static final int MONEY_SCALE = 2;

    private final AccountService accountService;
    private final KYCService kycService;

    @Override
    public EligibilityResponseDTO execute(EligibilityRequestDTO request) {
        // Account and KYC are read once. Every rule evaluates against the same snapshot,
        // so a concurrent update cannot make rule 3 and rule 7 disagree.
        Account account = accountService.findById(request.getAccountId());
        boolean kycVerified = kycService.findByAccountId(request.getAccountId())
                .map(kyc -> kyc.isVerified())
                .orElse(false);

        List<RejectionReason> reasons = evaluate(account, kycVerified, request.getAmount(), request.getCurrency());

        return EligibilityResponseDTO.builder()
                .accountId(account.getId())
                .eligible(reasons.isEmpty())
                .decision(reasons.isEmpty() ? Decision.APPROVED : Decision.REJECTED)
                .reasons(reasons)
                // Scale is normalised for output only, after every comparison is done.
                .amount(request.getAmount().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .availableBalance(account.availableBalance().setScale(MONEY_SCALE, RoundingMode.HALF_UP))
                .evaluatedAt(Instant.now().truncatedTo(ChronoUnit.SECONDS))
                .build();
    }

    private List<RejectionReason> evaluate(Account account, boolean kycVerified, BigDecimal amount, String currency) {
        List<RejectionReason> reasons = new ArrayList<>();

        // A CLOSED account is terminal: reporting the other reasons would imply the
        // payment could be fixed, and it cannot.
        if (account.getStatus() == AccountStatus.CLOSED) {
            reasons.add(RejectionReason.ACCOUNT_INACTIVE);
            return reasons;
        }

        if (account.getStatus() != AccountStatus.ACTIVE) {
            reasons.add(RejectionReason.ACCOUNT_INACTIVE);
        }
        if (!kycVerified) {
            reasons.add(RejectionReason.KYC_NOT_VERIFIED);
        }
        if (!account.getCurrency().equals(currency)) {
            reasons.add(RejectionReason.CURRENCY_MISMATCH);
        }

        // Ambiguity call: amount rules still run on a currency mismatch. The caller will
        // fix the currency and resubmit, and reporting every problem in one round-trip
        // beats a second rejection for a limit we already knew was breached.
        if (amount.compareTo(account.getMinTransaction()) < 0) {
            reasons.add(RejectionReason.AMOUNT_BELOW_MINIMUM);
        }
        if (account.getMaxTransaction() != null && amount.compareTo(account.getMaxTransaction()) > 0) {
            reasons.add(RejectionReason.AMOUNT_ABOVE_MAXIMUM);
        }
        if (amount.compareTo(account.availableBalance()) > 0) {
            reasons.add(RejectionReason.INSUFFICIENT_FUNDS);
        }
        if (account.getDailyLimit() != null
                && account.getDailySpent().add(amount).compareTo(account.getDailyLimit()) > 0) {
            reasons.add(RejectionReason.DAILY_LIMIT_EXCEEDED);
        }

        return reasons;
    }

}
