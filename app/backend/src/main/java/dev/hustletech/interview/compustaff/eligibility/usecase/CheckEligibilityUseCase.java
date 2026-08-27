package dev.hustletech.interview.compustaff.eligibility.usecase;

import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;

public interface CheckEligibilityUseCase {

    EligibilityResponseDTO execute(EligibilityRequestDTO request);

}
