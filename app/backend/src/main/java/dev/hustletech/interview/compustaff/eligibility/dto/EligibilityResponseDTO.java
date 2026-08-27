package dev.hustletech.interview.compustaff.eligibility.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import dev.hustletech.interview.compustaff.eligibility.domain.Decision;
import dev.hustletech.interview.compustaff.eligibility.domain.RejectionReason;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class EligibilityResponseDTO {

    private Long accountId;
    private boolean eligible;
    private Decision decision;
    private List<RejectionReason> reasons;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;

    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal availableBalance;

    private Instant evaluatedAt;

}
