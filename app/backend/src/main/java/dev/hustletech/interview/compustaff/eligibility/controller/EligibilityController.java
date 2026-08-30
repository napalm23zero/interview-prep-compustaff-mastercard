package dev.hustletech.interview.compustaff.eligibility.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;
import dev.hustletech.interview.compustaff.eligibility.usecase.CheckEligibilityUseCase;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/payments")
public class EligibilityController {

    private final CheckEligibilityUseCase checkEligibilityUseCase;

    public EligibilityController(CheckEligibilityUseCase checkEligibilityUseCase) {
        this.checkEligibilityUseCase = checkEligibilityUseCase;
    }

    /** A rejection is a successful answer: 200 with the reasons, never a 4xx. */
    @PostMapping("/eligibility")
    public EligibilityResponseDTO checkEligibility(@Valid @RequestBody EligibilityRequestDTO request) {
        return checkEligibilityUseCase.execute(request);
    }

}
