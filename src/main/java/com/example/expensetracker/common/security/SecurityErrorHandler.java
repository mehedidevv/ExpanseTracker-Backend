package com.example.expensetracker.common.security;

import com.example.expensetracker.common.exception.ErrorCode;
import com.example.expensetracker.common.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedException;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes errors raised by the security filter chain in the standard ApiError format:
 * 401 (no/bad token), 403 (access denied) and 400 for URLs the firewall rejects (e.g. a double slash
 * from a {@code baseUrl} that ends with "/").
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler, RequestRejectedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(request, response, ErrorCode.UNAUTHORIZED, "Authentication required");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(request, response, ErrorCode.FORBIDDEN, "Access denied");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       RequestRejectedException ex) throws IOException {
        String uri = request.getRequestURI();
        String message = uri.contains("//")
                ? "Invalid URL: it contains '//'. Remove the trailing '/' from baseUrl, e.g. http://localhost:8080/api/v1 (no '/' at the end)"
                : "Invalid URL";
        write(request, response, ErrorCode.VALIDATION_ERROR, message);
    }

    private void write(HttpServletRequest request, HttpServletResponse response, ErrorCode code,
                       String message) throws IOException {
        ApiError body = ApiError.of(code.status().value(), code.name(), message,
                request.getRequestURI(), List.of());
        response.setStatus(code.status().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
