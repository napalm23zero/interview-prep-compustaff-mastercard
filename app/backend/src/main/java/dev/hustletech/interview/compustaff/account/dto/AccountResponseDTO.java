package dev.hustletech.interview.compustaff.account.dto;


import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class AccountResponseDTO {
    private AccountStatus accountStatus;
}
