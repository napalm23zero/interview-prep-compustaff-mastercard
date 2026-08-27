package dev.hustletech.interview.compustaff.shared.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Builder;
import lombok.Getter;

/** Single error envelope for every failure the API can return. */
@Builder
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private Instant timestamp;
    private int status;
    private ErrorCode code;
    private String message;

    /** Present only for VALIDATION_ERROR. */
    private List<FieldError> fieldErrors;

    @Builder
    @Getter
    public static class FieldError {
        private String field;
        private String message;
    }

}
