package dev.hustletech.interview.compustaff.document.dto;

import dev.hustletech.interview.compustaff.document.domain.DocumentType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class DocumentDTO {

    private Long id;
    private DocumentType documentType;
    private String documentId;

}
