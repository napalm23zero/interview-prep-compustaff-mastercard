package dev.hustletech.interview.compustaff.kyc.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import dev.hustletech.interview.compustaff.document.domain.DocumentType;
import dev.hustletech.interview.compustaff.kyc.domain.KYC;

@DataJpaTest
class KYCRepositoryTest {

    @Autowired
    private KYCRepository repository;

    @Test
    void shouldFindKycByAccountIdWithItsDocument() {
        KYC kyc = repository.findByAccountId(1001L).orElseThrow();

        assertThat(kyc.isVerified()).isTrue();
        assertThat(kyc.getFirstName()).isEqualTo("Luke");
        assertThat(kyc.getBirthDate()).isEqualTo(LocalDate.of(1988, 3, 12));
        assertThat(kyc.getDocument().getDocumentType()).isEqualTo(DocumentType.PASSPORT);
    }

    @Test
    void shouldReportKycAsNotVerifiedForAccount1008() {
        KYC kyc = repository.findByAccountId(1008L).orElseThrow();

        assertThat(kyc.isVerified()).isFalse();
    }

    @Test
    void shouldReturnEmptyWhenAccountHasNoKycRecord() {
        assertThat(repository.findByAccountId(9999L)).isEmpty();
    }

}
