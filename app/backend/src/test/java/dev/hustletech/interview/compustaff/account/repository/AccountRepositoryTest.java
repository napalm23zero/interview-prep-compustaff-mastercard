package dev.hustletech.interview.compustaff.account.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.hustletech.interview.compustaff.account.domain.Account;
import dev.hustletech.interview.compustaff.account.domain.AccountStatus;

@DataJpaTest
class AccountRepositoryTest {

    @Autowired
    private AccountRepository repository;

    @Test
    void shouldLoadEverySeededAccountOrderedById() {
        List<Account> accounts = repository.findAllByOrderByIdAsc();

        assertThat(accounts).hasSize(11);
        assertThat(accounts.getFirst().getId()).isEqualTo(1001L);
        assertThat(accounts.getLast().getId()).isEqualTo(1011L);
    }

    @Test
    void shouldReadMonetaryColumnsWithTwoDecimalPlaces() {
        Account account = repository.findById(1001L).orElseThrow();

        // compareTo, never equals: new BigDecimal("5000.0").equals(new BigDecimal("5000.00"))
        // is false, because equals also compares scale.
        assertThat(account.getBalance()).isEqualByComparingTo("5000.00");
        assertThat(account.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(account.getCurrency()).isEqualTo("USD");
    }

    @Test
    void shouldSubtractHeldAmountFromAvailableBalance() {
        Account account = repository.findById(1007L).orElseThrow();

        assertThat(account.getBalance()).isEqualByComparingTo("5000.00");
        assertThat(account.availableBalance()).isEqualByComparingTo("100.00");
    }

    @Test
    void shouldKeepNullLimitsAsNullMeaningUnlimited() {
        Account account = repository.findById(1011L).orElseThrow();

        assertThat(account.getDailyLimit()).isNull();
        assertThat(account.getMaxTransaction()).isNull();
    }

    @Test
    void shouldStoreStatusAsReadableStringNotOrdinal() {
        Account closed = repository.findById(1009L).orElseThrow();

        assertThat(closed.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(closed.getDailySpent()).isEqualByComparingTo(new BigDecimal("99.00"));
    }

}
