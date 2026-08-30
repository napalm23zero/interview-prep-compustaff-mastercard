package dev.hustletech.interview.compustaff.eligibility.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

/** The HTTP contract over the real rules: status codes, JSON shape and the error envelope. */
@SpringBootTest
@AutoConfigureMockMvc
class EligibilityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn200WhenRejectedBecauseRejectionIsNotAProtocolError() throws Exception {
        mockMvc.perform(request("ACC-1004", "250.5", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(false))
                .andExpect(jsonPath("$.reasons[0]").value("ACCOUNT_INACTIVE"))
                // isString() is the point: a JSON number would lose precision in the browser.
                .andExpect(jsonPath("$.amount").isString())
                .andExpect(jsonPath("$.amount").value("250.50"))
                .andExpect(jsonPath("$.availableBalance").value("50.00"));
    }

    @Test
    void shouldReturn200AndNoReasonsWhenApproved() throws Exception {
        mockMvc.perform(request("ACC-1001", "250.00", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.reasons").isEmpty());
    }

    @Test
    void shouldReject400WithTheOffendingFieldWhenAmountIsNotPositive() throws Exception {
        mockMvc.perform(request("ACC-1001", "0.00", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("amount"));
    }

    @Test
    void shouldReject400WhenCurrencyIsLowercaseInsteadOfReportingCurrencyMismatch() throws Exception {
        mockMvc.perform(request("ACC-1001", "250.00", "usd"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("currency"));
    }

    @Test
    void shouldReturn400MalformedWhenAmountIsNotANumberNeverA500() throws Exception {
        mockMvc.perform(request("ACC-1001", "abc", "USD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.fieldErrors").doesNotExist());
    }

    @Test
    void shouldReturn404WithTheStandardEnvelopeWhenAccountDoesNotExist() throws Exception {
        mockMvc.perform(request("ACC-9999", "250.00", "USD"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private RequestBuilder request(String accountId, String amount, String currency) {
        String body = "{\"accountId\":\"%s\",\"amount\":\"%s\",\"currency\":\"%s\"}"
                .formatted(accountId, amount, currency);

        return post("/api/payments/eligibility").contentType(MediaType.APPLICATION_JSON).content(body);
    }

}
