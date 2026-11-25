package com.example.expensetracker.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Body of every successful response:
 * <pre>{ "success": true, "status": 200, "message": "...", "data": {...}, "timestamp": "...", "path": "..." }</pre>
 * {@code data} is left out when there is nothing to return (e.g. logout, delete).
 * Errors use {@link ApiError} with the same envelope fields and {@code success: false}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int status,
        String message,
        T data,
        Instant timestamp,
        String path) {

    /** 200 with data. */
    public static <T> ApiResponse<T> ok(String message, T data) {
        return of(200, message, data);
    }

    /** 200 without data. */
    public static ApiResponse<Void> ok(String message) {
        return of(200, message, null);
    }

    /** 201: pair with {@code @ResponseStatus(HttpStatus.CREATED)} on the controller method. */
    public static <T> ApiResponse<T> created(String message, T data) {
        return of(201, message, data);
    }

    private static <T> ApiResponse<T> of(int status, String message, T data) {
        return new ApiResponse<>(true, status, message, data, Instant.now(), currentPath());
    }

    private static String currentPath() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            HttpServletRequest request = attrs.getRequest();
            return request.getRequestURI();
        }
        return null;
    }
}
