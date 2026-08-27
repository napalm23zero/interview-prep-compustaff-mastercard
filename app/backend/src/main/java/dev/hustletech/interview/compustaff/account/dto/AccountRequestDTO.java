package dev.hustletech.interview.compustaff.account.dto;

import java.util.Date;

import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.kyc.dto.KYCRequestDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AccountRequestDTO {
    private Long id;
    private AccountStatus accountStatus;
    private String email;
    
    private Date createdAt;
    private Date updatedAt;
}
