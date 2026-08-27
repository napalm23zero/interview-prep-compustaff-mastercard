package dev.hustletech.interview.compustaff.account.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.account.service.AccountService;
import dev.hustletech.interview.compustaff.account.usecase.impl.CheckAccountStatusUseCaseImpl;

class CheckAccountStatusUseCaseTest {

    private AccountService accountService;
    private CheckAccountStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        useCase = new CheckAccountStatusUseCaseImpl(accountService);
    }

    @Test
    void shouldAcceptWhenAccountIsActive() {
        givenStatus(AccountStatus.ACTIVE);

        assertThat(useCase.execute(1001L)).isTrue();
    }

    @Test
    void shouldRejectWhenAccountIsClosed() {
        givenStatus(AccountStatus.CLOSED);

        assertThat(useCase.execute(1001L)).isFalse();
    }

    @Test
    void shouldRejectWhenAccountIsSuspended() {
        givenStatus(AccountStatus.SUSPENDED);

        assertThat(useCase.execute(1001L)).isFalse();
    }

    private void givenStatus(AccountStatus status) {
        when(accountService.findById(1001L)).thenReturn(Account.builder()
                .id(1001L)
                .status(status)
                .currency("USD")
                .balance(new BigDecimal("5000.00"))
                .heldAmount(BigDecimal.ZERO)
                .build());
    }

}
