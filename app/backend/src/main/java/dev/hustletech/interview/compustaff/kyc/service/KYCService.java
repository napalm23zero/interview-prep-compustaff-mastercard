package dev.hustletech.interview.compustaff.kyc.service;

import java.util.Optional;

import org.springframework.stereotype.Service;

import dev.hustletech.interview.compustaff.kyc.domain.KYC;
import dev.hustletech.interview.compustaff.kyc.repository.KYCRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KYCService {

    private final KYCRepository repository;

    public Optional<KYC> findByAccountId(Long accountId) {
        return repository.findByAccountId(accountId);
    }

}
