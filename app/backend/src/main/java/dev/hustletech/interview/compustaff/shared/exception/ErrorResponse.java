package dev.hustletech.interview.compustaff.shared.exception;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Single error envelope for every failure the API can return. fieldErrors is populated only
 * for VALIDATION_ERROR and left out of the JSON entirely otherwise.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Instant timestamp,
        int status,
        ErrorCode code,
        String message,
        List<FieldError> fieldErrors) {

    public record FieldError(String field, String message) {
    }

}
