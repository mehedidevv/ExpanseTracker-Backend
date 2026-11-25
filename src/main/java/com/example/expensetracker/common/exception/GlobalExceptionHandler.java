package com.example.expensetracker.common.exception;

import com.example.expensetracker.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Maps every exception to the PRD error format. Never leaks stack traces. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> handleApi(ApiException ex, HttpServletRequest req) {
        return build(ex.getCode(), ex.getMessage(), req, List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ApiError.FieldError(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, summarize(fields), req, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> handleParamValidation(HandlerMethodValidationException ex, HttpServletRequest req) {
        List<ApiError.FieldError> fields = ex.getParameterValidationResults().stream()
                .flatMap(r -> r.getResolvableErrors().stream()
                        .map(e -> new ApiError.FieldError(
                                r.getMethodParameter().getParameterName(), e.getDefaultMessage())))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, summarize(fields), req, fields);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String detail = ex.getMostSpecificCause().getMessage();
        String message = detail != null && detail.contains("not one of the values accepted for Enum")
                ? "Invalid value in request body: " + firstLine(detail)
                : "Request body is missing or is not valid JSON";
        return build(ErrorCode.VALIDATION_ERROR, message, req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR,
                "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'", req, List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR,
                "Missing required parameter '" + ex.getParameterName() + "'", req, List.of());
    }

    /** "email must be a valid email address; password size must be between 8 and 72". */
    private static String summarize(List<ApiError.FieldError> fields) {
        if (fields.isEmpty()) {
            return "Validation failed";
        }
        return fields.stream().map(f -> f.field() + " " + f.message()).distinct()
                .reduce((a, b) -> a + "; " + b).orElse("Validation failed");
    }

    private static String firstLine(String s) {
        int i = s.indexOf('\n');
        return i < 0 ? s : s.substring(0, i);
    }

    @ExceptionHandler(PropertyReferenceException.class)
    ResponseEntity<ApiError> handleBadSort(PropertyReferenceException ex, HttpServletRequest req) {
        return build(ErrorCode.VALIDATION_ERROR, "Unknown property: " + ex.getPropertyName(), req, List.of());
    }

    /** Safety net for unique/foreign-key races that slipped past service checks. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data integrity violation on {} {}: {}", req.getMethod(), req.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(ErrorCode.CONFLICT, "Request conflicts with existing data", req, List.of());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex, HttpServletRequest req) {
        return build(ErrorCode.NOT_FOUND, "Resource not found", req, List.of());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(405, "METHOD_NOT_ALLOWED",
                ex.getMessage(), req.getRequestURI(), List.of());
        return ResponseEntity.status(405).body(body);
    }

    /** Body sent without (or with a wrong) Content-Type: application/json. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ResponseEntity<ApiError> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(415, "UNSUPPORTED_MEDIA_TYPE",
                "Content-Type must be application/json", req.getRequestURI(), List.of());
        return ResponseEntity.status(415).body(body);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), ex);
        return build(ErrorCode.INTERNAL_ERROR, "Unexpected error", req, List.of());
    }

    private ResponseEntity<ApiError> build(ErrorCode code, String message, HttpServletRequest req,
                                           List<ApiError.FieldError> fieldErrors) {
        ApiError body = ApiError.of(code.status().value(), code.name(),
                message, req.getRequestURI(), fieldErrors);
        return ResponseEntity.status(code.status()).body(body);
    }
}
