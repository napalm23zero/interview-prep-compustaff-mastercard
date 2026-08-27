package dev.hustletech.interview.compustaff.account.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String holderName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal heldAmount;

    /** Null means unlimited, not zero. */
    @Column(precision = 12, scale = 2)
    private BigDecimal dailyLimit;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal dailySpent;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal minTransaction;

    /** Null means unlimited, not zero. */
    @Column(precision = 12, scale = 2)
    private BigDecimal maxTransaction;

    /** Balance alone is not spendable: pending transactions hold part of it. */
    public BigDecimal availableBalance() {
        return balance.subtract(heldAmount);
    }

}
