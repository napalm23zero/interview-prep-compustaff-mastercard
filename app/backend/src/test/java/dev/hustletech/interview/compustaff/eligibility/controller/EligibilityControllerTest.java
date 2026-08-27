package dev.hustletech.interview.compustaff.eligibility.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import dev.hustletech.interview.compustaff.eligibility.domain.Decision;
import dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason;
import dev.hustletech.interview.compustaff.eligibility.dto.EligibilityResponseDTO;
import dev.hustletech.interview.compustaff.eligibility.usecase.CheckEligibilityUseCase;
import dev.hustletech.interview.compustaff.shared.exception.AccountNotFoundException;

@WebMvcTest(EligibilityController.class)
class EligibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CheckEligibilityUseCase checkEligibilityUseCase;

    @Test
    void shouldReturn200WhenPaymentIsRejectedBecauseRejectionIsNotAProtocolError() throws Exception {
        when(checkEligibilityUseCase.execute(any())).thenReturn(rejected());

        mockMvc.perform(request("{\"accountId\":1008,\"amount\":\"250.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.decision").value("REJECTED"))
                .andExpect(jsonPath("$.reasons[0]").value("KYC_NOT_VERIFIED"))
                .andExpect(jsonPath("$.reasons[1]").value("INSUFFICIENT_FUNDS"));
    }

    @Test
    void shouldSerialiseMonetaryFieldsAsStringsWithTwoDecimalPlaces() throws Exception {
        when(checkEligibilityUseCase.execute(any())).thenReturn(rejected());

        mockMvc.perform(request("{\"accountId\":1008,\"amount\":\"250.5\",\"currency\":\"USD\"}"))
                .andExpect(status().isOk())
                // isString() is the point: a JSON number would lose precision in the browser.
                .andExpect(jsonPath("$.amount").isString())
                .andExpect(jsonPath("$.amount").value("250.50"))
                .andExpect(jsonPath("$.availableBalance").isString())
                .andExpect(jsonPath("$.availableBalance").value("50.00"));
    }

    @Test
    void shouldReject400WhenAmountIsZero() throws Exception {
        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"0.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    @Test
    void shouldReject400WhenAmountIsNegative() throws Exception {
        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"-5.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReject400WhenAmountHasMoreThanTwoDecimalPlaces() throws Exception {
        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"250.500\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldAcceptAmountWithOneDecimalPlace() throws Exception {
        when(checkEligibilityUseCase.execute(any())).thenReturn(rejected());

        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"250.5\",\"currency\":\"USD\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReject400WhenCurrencyIsLowercaseInsteadOfReportingCurrencyMismatch() throws Exception {
        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"250.00\",\"currency\":\"usd\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currency"));
    }

    @Test
    void shouldReject400WhenCurrencyHasTwoLetters() throws Exception {
        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"250.00\",\"currency\":\"US\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void shouldReturn400MalformedRequestWhenAmountIsNotANumberNeverA500() throws Exception {
        mockMvc.perform(request("{\"accountId\":1001,\"amount\":\"abc\",\"currency\":\"USD\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void shouldReturn404WithTheStandardEnvelopeWhenAccountDoesNotExist() throws Exception {
        when(checkEligibilityUseCase.execute(any())).thenThrow(new AccountNotFoundException(9999L));

        mockMvc.perform(request("{\"accountId\":9999,\"amount\":\"250.00\",\"currency\":\"USD\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // --- helpers ---

    private org.springframework.test.web.servlet.RequestBuilder request(String body) {
        return post("/api/payments/eligibility").contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private EligibilityResponseDTO rejected() {
        return EligibilityResponseDTO.builder()
                .accountId(1008L)
                .eligible(false)
                .decision(Decision.REJECTED)
                .reasons(List.of(RejectionReason.KYC_NOT_VERIFIED, RejectionReason.INSUFFICIENT_FUNDS))
                .amount(new BigDecimal("250.50"))
                .availableBalance(new BigDecimal("50.00"))
                .evaluatedAt(Instant.parse("2026-08-27T14:22:05Z"))
                .build();
    }

}
