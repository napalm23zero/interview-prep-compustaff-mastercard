package dev.hustletech.interview.compustaff.account.usecase.impl;

import org.springframework.stereotype.Component;

import dev.hustletech.interview.compustaff.account.domain.AccountStatus;
import dev.hustletech.interview.compustaff.account.service.AccountService;
import dev.hustletech.interview.compustaff.account.usecase.CheckAccountStatusUseCase;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CheckAccountStatusUseCaseImpl implements CheckAccountStatusUseCase {

    private final AccountService service;

    @Override
    public boolean execute(Long accountId) {
        return service.findById(accountId).getStatus() == AccountStatus.ACTIVE;
    }

}
