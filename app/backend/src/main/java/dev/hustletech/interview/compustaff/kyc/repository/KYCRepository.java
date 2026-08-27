package dev.hustletech.interview.compustaff.kyc.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import dev.hustletech.interview.compustaff.kyc.domain.KYC;

@Repository
public interface KYCRepository extends JpaRepository<KYC, Long> {

    Optional<KYC> findByAccountId(Long accountId);

}
