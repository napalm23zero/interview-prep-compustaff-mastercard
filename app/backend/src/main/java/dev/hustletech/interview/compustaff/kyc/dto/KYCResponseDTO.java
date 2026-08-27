package dev.hustletech.interview.compustaff.kyc.dto;

import java.time.LocalDate;

import dev.hustletech.interview.compustaff.document.dto.DocumentDTO;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class KYCResponseDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private LocalDate birthDate;
    private DocumentDTO document;

}
