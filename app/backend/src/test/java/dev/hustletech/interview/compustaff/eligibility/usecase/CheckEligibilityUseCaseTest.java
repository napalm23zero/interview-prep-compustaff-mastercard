package dev.hustletech.interview.compustaff.eligibility.usecase;

import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.ACCOUNT_INACTIVE;
import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.AMOUNT_ABOVE_MAXIMUM;
import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.AMOUNT_BELOW_MINIMUM;
import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.CURRENCY_MISMATCH;
import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.DAILY_LIMIT_EXCEEDED;
import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.INSUFFICIENT_FUNDS;
import static dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason.KYC_NOT_VERIFIED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import dev.hustletech.interview.compustaff.account.repository.AccountRepository;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;
import dev.hustletech.interview.compustaff.shared.exception.AccountNotFoundException;

/** The seeded accounts are the fixture, so the rules run against real data and nothing is mocked. */
class CheckEligibilityUseCaseTest {

    private final CheckEligibilityUseCase useCase = new CheckEligibilityUseCase(new AccountRepository());

    @Test
    void shouldApproveWhenNoRuleIsViolated() {
        EligibilityResponseDTO response = evaluate("ACC-1001", "250.00", "USD");

        assertThat(response.eligible()).isTrue();
        assertThat(response.reasons()).isEmpty();
    }

    @Test
    void shouldIgnoreHeldFundsWhenCheckingBalance() {
        // 5000.00 balance, 4900.00 of it held: only 100.00 is spendable.
        EligibilityResponseDTO response = evaluate("ACC-1003", "250.00", "USD");

        assertThat(response.reasons()).containsExactly(INSUFFICIENT_FUNDS);
        assertThat(response.availableBalance()).isEqualTo("100.00");
    }

    @Test
    void shouldRejectWhenRequestCurrencyDiffersFromAccountCurrency() {
        assertThat(evaluate("ACC-1002", "250.00", "USD").reasons()).containsExactly(CURRENCY_MISMATCH);
    }

    @Test
    void shouldAccumulateEveryApplicableReasonInDeclarationOrder() {
        assertThat(evaluate("ACC-1004", "250.00", "USD").reasons())
                .containsExactly(ACCOUNT_INACTIVE, KYC_NOT_VERIFIED, INSUFFICIENT_FUNDS, DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void shouldReturnOnlyAccountInactiveWhenClosedBecauseClosedIsTerminal() {
        // KYC, currency, funds and the daily limit all fail too, and none may be reported.
        assertThat(evaluate("ACC-1005", "250.00", "USD").reasons()).containsExactly(ACCOUNT_INACTIVE);
    }

    @Test
    void shouldTreatNullLimitsAsUnlimitedNotZero() {
        assertThat(evaluate("ACC-1006", "999999.99", "USD").reasons()).isEmpty();
    }

    @Test
    void shouldApproveOnTheBoundsBecauseMinimumMaximumAndDailyLimitAreInclusive() {
        assertThat(evaluate("ACC-1001", "1.00", "USD").reasons()).isEmpty();
        // 2500.00 is exactly maxTransaction and exactly the daily limit.
        assertThat(evaluate("ACC-1001", "2500.00", "USD").reasons()).isEmpty();
    }

    @Test
    void shouldRejectJustOutsideTheBounds() {
        assertThat(evaluate("ACC-1001", "0.50", "USD").reasons()).containsExactly(AMOUNT_BELOW_MINIMUM);
        assertThat(evaluate("ACC-1001", "2500.01", "USD").reasons())
                .containsExactly(AMOUNT_ABOVE_MAXIMUM, DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void shouldCompareAmountsByValueSoTrailingZeroesDoNotChangeTheDecision() {
        // "1.0".equals("1.00") is false and compareTo is 0. The minimum is 1.00, so only
        // compareTo keeps this eligible.
        assertThat(evaluate("ACC-1001", "1.0", "USD").eligible()).isTrue();
    }

    @Test
    void shouldPropagateAccountNotFoundInsteadOfReportingItAsAReason() {
        assertThatThrownBy(() -> evaluate("ACC-9999", "250.00", "USD"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    private EligibilityResponseDTO evaluate(String accountId, String amount, String currency) {
        return useCase.execute(new EligibilityRequestDTO(accountId, new BigDecimal(amount), currency));
    }

}
