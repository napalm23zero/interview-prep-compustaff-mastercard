package dev.hustletech.interview.compustaff.eligibility.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * The caller sends an account id and what it wants to pay. Account status, KYC and
 * balances are loaded server-side: the caller must not be able to state them.
 */
public record EligibilityRequestDTO(

        @NotBlank(message = "must not be blank")
        String accountId,

        @NotNull(message = "must not be null")
        @DecimalMin(value = "0.00", inclusive = false, message = "must be greater than 0")
        @Digits(integer = 12, fraction = 2, message = "must have at most 2 decimal places")
        BigDecimal amount,

        @NotNull(message = "must not be null")
        @Pattern(regexp = "^[A-Z]{3}$", message = "must be 3 uppercase letters")
        String currency) {

}
