package dev.hustletech.interview.compustaff.eligibility.dto;

import java.util.List;

import dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason;

/**
 * Monetary fields are String, not BigDecimal: a JSON number is an IEEE-754 double and the
 * browser would lose precision parsing it. The type says so at the boundary.
 */
public record EligibilityResponseDTO(
        boolean eligible,
        List<RejectionReason> reasons,
        String amount,
        String availableBalance) {

}
