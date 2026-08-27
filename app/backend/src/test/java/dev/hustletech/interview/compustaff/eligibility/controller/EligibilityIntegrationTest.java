package dev.hustletech.interview.compustaff.eligibility.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * End-to-end over the seeded H2 data. The @WebMvcTest suite mocks the use case, so this
 * is the only place where rules, persistence and JSON are exercised together.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EligibilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldApproveLukeSkywalker() throws Exception {
        mockMvc.perform(request(1001L, "250.00", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.decision").value("APPROVED"))
                .andExpect(jsonPath("$.reasons").isEmpty())
                .andExpect(jsonPath("$.amount").value("250.00"));
    }

    @Test
    void shouldReturnEveryReasonForMaceWinduInDeclarationOrder() throws Exception {
        mockMvc.perform(request(1008L, "250.00", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasons.length()").value(3))
                .andExpect(jsonPath("$.reasons[0]").value("KYC_NOT_VERIFIED"))
                .andExpect(jsonPath("$.reasons[1]").value("INSUFFICIENT_FUNDS"))
                .andExpect(jsonPath("$.reasons[2]").value("DAILY_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.availableBalance").value("50.00"));
    }

    @Test
    void shouldReturnOnlyAccountInactiveForPalpatineBecauseClosedIsTerminal() throws Exception {
        mockMvc.perform(request(1009L, "250.00", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reasons.length()").value(1))
                .andExpect(jsonPath("$.reasons[0]").value("ACCOUNT_INACTIVE"));
    }

    @Test
    void shouldNeverReturn500ForAhsokaTanoWhoseLimitsAreNull() throws Exception {
        mockMvc.perform(request(1011L, "999999.99", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true));
    }

    @Test
    void shouldEchoAmountWithTwoDecimalPlaces() throws Exception {
        mockMvc.perform(request(1001L, "1.0", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.eligible").value(true))
                .andExpect(jsonPath("$.amount").value("1.00"));
    }

    @Test
    void shouldReturn404WhenAccountIsNotSeeded() throws Exception {
        mockMvc.perform(request(9999L, "250.00", "USD"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACCOUNT_NOT_FOUND"));
    }

    private org.springframework.test.web.servlet.RequestBuilder request(Long accountId, String amount,
            String currency) {

        String body = "{\"accountId\":%d,\"amount\":\"%s\",\"currency\":\"%s\"}"
                .formatted(accountId, amount, currency);

        return post("/api/payments/eligibility").contentType(MediaType.APPLICATION_JSON).content(body);
    }

}
