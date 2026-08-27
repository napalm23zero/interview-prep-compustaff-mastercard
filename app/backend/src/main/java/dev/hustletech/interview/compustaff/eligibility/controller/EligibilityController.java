package dev.hustletech.interview.compustaff.eligibility.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityRequestDTO;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;
import dev.hustletech.interview.compustaff.eligibility.usecase.CheckEligibilityUseCase;
import dev.hustletech.interview.compustaff.shared.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class EligibilityController {

    private final CheckEligibilityUseCase checkEligibilityUseCase;


    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Decision reached (approved or rejected)"),
            @ApiResponse(responseCode = "400", description = "Validation failure or malformed body", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Unknown account", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/eligibility")
    public ResponseEntity<EligibilityResponseDTO> checkEligibility(
            @Valid @RequestBody EligibilityRequestDTO request) {

        return ResponseEntity.ok(checkEligibilityUseCase.execute(request));
    }

}
