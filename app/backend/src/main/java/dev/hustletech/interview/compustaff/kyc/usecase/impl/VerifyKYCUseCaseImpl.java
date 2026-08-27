package dev.hustletech.interview.compustaff.kyc.usecase.impl;

import org.springframework.stereotype.Component;

import dev.hustletech.interview.compustaff.kyc.domain.KYC;
import dev.hustletech.interview.compustaff.kyc.service.KYCService;
import dev.hustletech.interview.compustaff.kyc.usecase.VerifyKYCUseCase;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class VerifyKYCUseCaseImpl implements VerifyKYCUseCase {

    private final KYCService service;

    /** No KYC record at all counts as not verified, never as an error. */
    @Override
    public boolean execute(Long accountId) {
        return service.findByAccountId(accountId).map(KYC::isVerified).orElse(false);
    }

}
