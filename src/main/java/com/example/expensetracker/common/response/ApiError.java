package com.example.expensetracker.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

/**
 * Body of every failed request:
 * <pre>{ "success": false, "status": 400, "code": "VALIDATION_ERROR", "message": "...", "fieldErrors": [...],
 *   "timestamp": "...", "path": "..." }</pre>
 * {@code fieldErrors} is left out when empty.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        boolean success,
        int status,
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<FieldError> fieldErrors,
        Instant timestamp,
        String path) {

    public static ApiError of(int status, String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(false, status, code, message, fieldErrors, Instant.now(), path);
    }

    public record FieldError(String field, String message) {
    }
}
