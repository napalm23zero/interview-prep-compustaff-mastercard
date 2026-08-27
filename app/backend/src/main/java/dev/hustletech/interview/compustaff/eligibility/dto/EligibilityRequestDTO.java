package dev.hustletech.interview.compustaff.eligibility.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The caller sends an account id and what it wants to pay. Account status, KYC and
 * balances are loaded server-side: the caller must not be able to state them.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EligibilityRequestDTO {

    @NotNull(message = "must not be null")
    private Long accountId;

    @NotNull(message = "must not be null")
    @DecimalMin(value = "0.00", inclusive = false, message = "must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "must have at most 2 decimal places")
    private BigDecimal amount;

    @NotNull(message = "must not be null")
    @Pattern(regexp = "^[A-Z]{3}$", message = "must be 3 uppercase letters")
    private String currency;

}
