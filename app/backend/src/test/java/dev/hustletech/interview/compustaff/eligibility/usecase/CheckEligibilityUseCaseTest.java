package dev.hustletech.interview.compustaff.eligibility.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.account.service.AccountService;
import dev.hustletech.interview.compustaff.eligibility.domain.Decision;
import dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;
import dev.hustletech.interview.compustaff.eligibility.usecase.impl.CheckEligibilityUseCaseImpl;
import dev.hustletech.interview.compustaff.kyc.domain.KYC;
import dev.hustletech.interview.compustaff.kyc.service.KYCService;
import dev.hustletech.interview.compustaff.shared.exception.AccountNotFoundException;

class CheckEligibilityUseCaseTest {

    private AccountService accountService;
    private KYCService kycService;
    private CheckEligibilityUseCase useCase;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        kycService = mock(KYCService.class);
        useCase = new CheckEligibilityUseCaseImpl(accountService, kycService);
    }

    @Test
    void shouldApproveWhenNoRuleIsViolated() {
        givenAccount(account(1001L, AccountStatus.ACTIVE, "USD", "5000.00", "0.00", "10000.00", "0.00"), true);

        EligibilityResponseDTO response = evaluate(1001L, "250.00", "USD");

        assertThat(response.isEligible()).isTrue();
        assertThat(response.getDecision()).isEqualTo(Decision.APPROVED);
        assertThat(response.getReasons()).isEmpty();
    }

    @Test
    void shouldRejectWhenAmountExceedsAvailableBalance() {
        givenAccount(account(1002L, AccountStatus.ACTIVE, "USD", "100.00", "0.00", "10000.00", "0.00"), true);

        assertThat(evaluate(1002L, "250.00", "USD").getReasons())
                .containsExactly(RejectionReason.INSUFFICIENT_FUNDS);
    }

    @Test
    void shouldRejectWhenRequestCurrencyDiffersFromAccountCurrency() {
        givenAccount(account(1005L, AccountStatus.ACTIVE, "EUR", "5000.00", "0.00", "10000.00", "0.00"), true);

        assertThat(evaluate(1005L, "250.00", "USD").getReasons())
                .containsExactly(RejectionReason.CURRENCY_MISMATCH);
    }

    @Test
    void shouldIgnoreHeldFundsWhenCheckingBalance() {
        givenAccount(account(1007L, AccountStatus.ACTIVE, "USD", "5000.00", "4900.00", "10000.00", "0.00"), true);

        EligibilityResponseDTO response = evaluate(1007L, "250.00", "USD");

        assertThat(response.getReasons()).containsExactly(RejectionReason.INSUFFICIENT_FUNDS);
        assertThat(response.getAvailableBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldAccumulateEveryApplicableReasonInDeclarationOrder() {
        givenAccount(account(1008L, AccountStatus.ACTIVE, "USD", "50.00", "0.00", "1000.00", "990.00"), false);

        assertThat(evaluate(1008L, "250.00", "USD").getReasons()).containsExactly(
                RejectionReason.KYC_NOT_VERIFIED,
                RejectionReason.INSUFFICIENT_FUNDS,
                RejectionReason.DAILY_LIMIT_EXCEEDED);
    }

    @Test
    void shouldAccumulateReasonsWhenAccountIsSuspendedBecauseSuspendedIsNotTerminal() {
        givenAccount(account(1010L, AccountStatus.SUSPENDED, "USD", "5000.00", "0.00", "10000.00", "0.00"), false);

        assertThat(evaluate(1010L, "250.00", "USD").getReasons()).containsExactly(
                RejectionReason.ACCOUNT_INACTIVE,
                RejectionReason.KYC_NOT_VERIFIED);
    }

    @Test
    void shouldReturnOnlyAccountInactiveWhenAccountIsClosedBecauseClosedIsTerminal() {
        // KYC, currency and funds all fail too, and none of them may be reported.
        givenAccount(account(1009L, AccountStatus.CLOSED, "EUR", "10.00", "0.00", "100.00", "99.00"), false);

        assertThat(evaluate(1009L, "250.00", "USD").getReasons())
                .containsExactly(RejectionReason.ACCOUNT_INACTIVE);
    }

    @Test
    void shouldTreatNullLimitsAsUnlimitedNotZero() {
        Account unlimited = account(1011L, AccountStatus.ACTIVE, "USD", "1000000.00", "0.00", null, "0.00");
        unlimited.setMaxTransaction(null);
        givenAccount(unlimited, true);

        assertThat(evaluate(1011L, "999999.99", "USD").getReasons()).isEmpty();
    }

    @Test
    void shouldApproveAmountEqualToMaxTransactionBecauseTheBoundIsInclusive() {
        givenAccount(account(1001L, AccountStatus.ACTIVE, "USD", "5000.00", "0.00", "10000.00", "0.00"), true);

        assertThat(evaluate(1001L, "2500.00", "USD").getReasons()).isEmpty();
    }

    @Test
    void shouldApproveAmountEqualToMinTransactionBecauseTheBoundIsInclusive() {
        givenAccount(account(1001L, AccountStatus.ACTIVE, "USD", "5000.00", "0.00", "10000.00", "0.00"), true);

        assertThat(evaluate(1001L, "1.00", "USD").getReasons()).isEmpty();
    }

    @Test
    void shouldApproveWhenDailySpentPlusAmountEqualsTheDailyLimit() {
        givenAccount(account(1006L, AccountStatus.ACTIVE, "USD", "5000.00", "0.00", "1000.00", "900.00"), true);

        assertThat(evaluate(1006L, "100.00", "USD").getReasons()).isEmpty();
    }

    @Test
    void shouldEchoAmountWithTwoDecimalPlacesWithoutChangingTheComparison() {
        givenAccount(account(1001L, AccountStatus.ACTIVE, "USD", "5000.00", "0.00", "10000.00", "0.00"), true);

        EligibilityResponseDTO response = evaluate(1001L, "1.0", "USD");

        assertThat(response.isEligible()).isTrue();
        assertThat(response.getAmount().toPlainString()).isEqualTo("1.00");
    }

    @Test
    void shouldThrowAccountNotFoundWhenAccountDoesNotExist() {
        when(accountService.findById(9999L)).thenThrow(new AccountNotFoundException(9999L));

        assertThatThrownBy(() -> evaluate(9999L, "250.00", "USD"))
                .isInstanceOf(AccountNotFoundException.class);
    }

    // --- helpers ---

    private EligibilityResponseDTO evaluate(Long accountId, String amount, String currency) {
        return useCase.execute(EligibilityRequestDTO.builder()
                .accountId(accountId)
                .amount(new BigDecimal(amount))
                .currency(currency)
                .build());
    }

    private void givenAccount(Account account, boolean kycVerified) {
        when(accountService.findById(account.getId())).thenReturn(account);
        when(kycService.findByAccountId(any()))
                .thenReturn(Optional.of(KYC.builder().verified(kycVerified).build()));
    }

    /** Mirrors the seed rows in data.sql; min is always 1.00 and max always 2500.00 there. */
    private Account account(Long id, AccountStatus status, String currency, String balance, String held,
            String dailyLimit, String dailySpent) {

        return Account.builder()
                .id(id)
                .holderName("Test Holder")
                .status(status)
                .currency(currency)
                .balance(new BigDecimal(balance))
                .heldAmount(new BigDecimal(held))
                .dailyLimit(dailyLimit == null ? null : new BigDecimal(dailyLimit))
                .dailySpent(new BigDecimal(dailySpent))
                .minTransaction(new BigDecimal("1.00"))
                .maxTransaction(new BigDecimal("2500.00"))
                .build();
    }

}
