package org.kshrd.hrdroomservice.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApi(ApiException ex, HttpServletRequest req) {
        Map<String, Object> details = null;
        if (ex.getField() != null) {
            details = Map.of("field", ex.getField());
        }
        return ResponseEntity.status(ex.getStatus())
                .body(
                        ApiResponse.error(
                                ex.getStatus().value(),
                                ex.getMessage(),
                                ex.getErrorCode(),
                                req.getRequestURI(),
                                details));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String message =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        List<Map<String, String>> violations =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(this::toViolation)
                        .collect(Collectors.toList());
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("violations", violations);
        return ResponseEntity.badRequest()
                .body(
                        ApiResponse.error(
                                HttpStatus.BAD_REQUEST.value(),
                                message,
                                "VALIDATION_ERROR",
                                req.getRequestURI(),
                                details));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(
            AuthenticationException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ApiResponse.error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Authentication is required for this resource.",
                                "UNAUTHORIZED",
                                req.getRequestURI()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleDenied(
            AccessDeniedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                HttpStatus.FORBIDDEN.value(),
                                "You do not have permission to perform this action.",
                                "ACCESS_DENIED",
                                req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(
            Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception for request path={}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(
                        ApiResponse.error(
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                "An unexpected error occurred",
                                "INTERNAL_ERROR",
                                req.getRequestURI()));
    }

    private Map<String, String> toViolation(FieldError fieldError) {
        Map<String, String> violation = new LinkedHashMap<>();
        violation.put("field", fieldError.getField());
        violation.put("message", fieldError.getDefaultMessage());
        return violation;
    }
}
