package org.kshrd.hrdroomservice.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

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
        return respond(
                HttpStatus.BAD_REQUEST,
                toValidationMessage(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(this::toViolation)
                                .toList()),
                "VALIDATION_ERROR",
                req,
                violationsDetails(
                        ex.getBindingResult().getFieldErrors().stream()
                                .map(this::toViolation)
                                .toList()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException ex, HttpServletRequest req) {
        List<Map<String, String>> violations =
                ex.getParameterValidationResults().stream()
                        .map(
                                result -> {
                                    Map<String, String> violation = new LinkedHashMap<>();
                                    violation.put(
                                            "field",
                                            result.getMethodParameter().getParameterName());
                                    String message =
                                            result.getResolvableErrors().stream()
                                                    .map(
                                                            error ->
                                                                    Objects.requireNonNullElse(
                                                                            error
                                                                                    .getDefaultMessage(),
                                                                            "Validation failed"))
                                                    .collect(Collectors.joining("; "));
                                    violation.put(
                                            "message",
                                            message.isBlank() ? "Validation failed" : message);
                                    return violation;
                                })
                        .toList();
        log.warn("Method validation failed for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.BAD_REQUEST,
                toValidationMessage(violations),
                "VALIDATION_ERROR",
                req,
                violationsDetails(violations));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        List<Map<String, String>> violations =
                ex.getConstraintViolations().stream().map(this::toViolation).toList();
        log.warn("Constraint validation failed for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.BAD_REQUEST,
                toValidationMessage(violations),
                "VALIDATION_ERROR",
                req,
                violationsDetails(violations));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        String field = extractJsonField(ex);
        if (field != null) {
            details.put("field", field);
        }
        String expectedType = extractExpectedType(ex);
        if (expectedType != null) {
            details.put("expectedType", expectedType);
        }
        log.warn("Malformed request body for path={}: {}", req.getRequestURI(), ex.getMessage());
        return respond(
                HttpStatus.BAD_REQUEST,
                "Malformed request body.",
                "MALFORMED_REQUEST",
                req,
                details.isEmpty() ? null : details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("field", ex.getName());
        if (ex.getRequiredType() != null) {
            details.put("expectedType", ex.getRequiredType().getSimpleName());
        }
        log.warn("Type mismatch for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.BAD_REQUEST,
                "Request parameter type mismatch.",
                "TYPE_MISMATCH",
                req,
                details);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        Map<String, Object> details = Map.of("field", ex.getParameterName());
        log.warn("Missing request parameter for path={}", req.getRequestURI());
        return respond(
                HttpStatus.BAD_REQUEST,
                "Missing required request parameter.",
                "MISSING_PARAMETER",
                req,
                details);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingPathVariable(
            MissingPathVariableException ex, HttpServletRequest req) {
        Map<String, Object> details = Map.of("field", ex.getVariableName());
        log.warn("Missing path variable for path={}", req.getRequestURI());
        return respond(
                HttpStatus.BAD_REQUEST,
                "Missing required path variable.",
                "MISSING_PATH_VARIABLE",
                req,
                details);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        HttpHeaders headers = new HttpHeaders();
        Set<HttpMethod> methods = ex.getSupportedHttpMethods();
        if (methods != null && !methods.isEmpty()) {
            headers.setAllow(methods);
        }
        log.warn("Method not allowed for request path={}", req.getRequestURI());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .headers(headers)
                .body(
                        ApiResponse.error(
                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                "HTTP method is not allowed for this endpoint.",
                                "METHOD_NOT_ALLOWED",
                                req.getRequestURI()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        log.warn("Unsupported media type for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Content type is not supported.",
                "UNSUPPORTED_MEDIA_TYPE",
                req,
                null);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest req) {
        log.warn("Not acceptable media type for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.NOT_ACCEPTABLE,
                "Requested response media type is not acceptable.",
                "NOT_ACCEPTABLE",
                req,
                null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handlePayloadTooLarge(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("Payload too large for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.CONTENT_TOO_LARGE, "Payload too large.", "PAYLOAD_TOO_LARGE", req, null);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ApiResponse<Void>> handleNoRoute(Exception ex, HttpServletRequest req) {
        log.warn("No route found for request path={}", req.getRequestURI());
        return respond(HttpStatus.NOT_FOUND, "Resource not found.", "NOT_FOUND", req, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Data conflict for request path={}", req.getRequestURI());
        return respond(
                HttpStatus.CONFLICT,
                "The request conflicts with existing data.",
                "CONFLICT",
                req,
                null);
    }

    @ExceptionHandler(AuthenticationServiceException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationService(
            AuthenticationServiceException ex, HttpServletRequest req) {
        log.warn("Identity provider unavailable for request path={}", req.getRequestURI(), ex);
        return respond(
                HttpStatus.BAD_GATEWAY,
                "Identity provider is unavailable.",
                "IDP_UNAVAILABLE",
                req,
                null);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(
            AuthenticationException ex, HttpServletRequest req) {
        if (ex instanceof OAuth2AuthenticationException oae) {
            return handleOAuth2Authentication(oae, req);
        }
        log.warn("Authentication failed for request path={}", req.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(
                        ApiResponse.error(
                                HttpStatus.UNAUTHORIZED.value(),
                                "Authentication is required for this resource.",
                                "UNAUTHORIZED",
                                req.getRequestURI()));
    }

    private ResponseEntity<ApiResponse<Void>> handleOAuth2Authentication(
            OAuth2AuthenticationException ex, HttpServletRequest req) {
        OAuth2Error error = ex.getError();
        if (error == null) {
            error = new OAuth2Error("invalid_request");
        }
        HttpStatus status =
                error instanceof BearerTokenError bte
                        ? bte.getHttpStatus()
                        : HttpStatus.UNAUTHORIZED;
        String description = Objects.requireNonNullElse(error.getDescription(), "");
        String message = "Bearer token authentication failed.";
        if (StringUtils.hasText(description)) {
            message = description;
        }
        log.warn(
                "OAuth2 authentication failed for request path={} error={}",
                req.getRequestURI(),
                Objects.requireNonNullElse(error.getErrorCode(), "unknown"));
        return ResponseEntity.status(status)
                .header(HttpHeaders.WWW_AUTHENTICATE, buildWwwAuthenticateHeader(error))
                .body(
                        ApiResponse.error(
                                status.value(), message, "AUTH_TOKEN_ERROR", req.getRequestURI()));
    }

    private static String buildWwwAuthenticateHeader(OAuth2Error error) {
        StringJoiner joiner = new StringJoiner(", ", "Bearer ", "");
        joiner.add(
                "error=\""
                        + escapeQuoted(
                                Objects.requireNonNullElse(error.getErrorCode(), "server_error"))
                        + "\"");
        if (StringUtils.hasText(error.getDescription())) {
            joiner.add("error_description=\"" + escapeQuoted(error.getDescription()) + "\"");
        }
        if (StringUtils.hasText(error.getUri())) {
            joiner.add("error_uri=\"" + escapeQuoted(error.getUri()) + "\"");
        }
        if (error instanceof BearerTokenError bearer && StringUtils.hasText(bearer.getScope())) {
            joiner.add("scope=\"" + escapeQuoted(bearer.getScope()) + "\"");
        }
        return joiner.toString();
    }

    private static String escapeQuoted(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
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

    private Map<String, String> toViolation(ConstraintViolation<?> violation) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("field", violation.getPropertyPath().toString());
        row.put("message", violation.getMessage());
        return row;
    }

    private Map<String, Object> violationsDetails(List<Map<String, String>> violations) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("violations", violations);
        return details;
    }

    private String toValidationMessage(List<Map<String, String>> violations) {
        return violations.stream()
                .map(v -> v.get("field") + ": " + v.get("message"))
                .collect(Collectors.joining("; "));
    }

    private ResponseEntity<ApiResponse<Void>> respond(
            HttpStatusCode status,
            String message,
            String errorCode,
            HttpServletRequest req,
            Map<String, Object> details) {
        return ResponseEntity.status(status)
                .body(
                        ApiResponse.error(
                                status.value(), message, errorCode, req.getRequestURI(), details));
    }

    private String extractJsonField(Throwable throwable) {
        Object path = invokeReflective(throwable, "getPath");
        if (!(path instanceof List<?> pathList) || pathList.isEmpty()) {
            Throwable cause = throwable.getCause();
            if (cause != null && cause != throwable) {
                return extractJsonField(cause);
            }
            return null;
        }
        List<String> parts =
                pathList.stream()
                        .map(
                                ref -> {
                                    Object fieldName = invokeReflective(ref, "getFieldName");
                                    if (fieldName instanceof String field && !field.isBlank()) {
                                        return field;
                                    }
                                    Object idx = invokeReflective(ref, "getIndex");
                                    if (idx instanceof Integer index && index >= 0) {
                                        return "[" + index + "]";
                                    }
                                    return null;
                                })
                        .filter(Objects::nonNull)
                        .toList();
        if (parts.isEmpty()) {
            return null;
        }
        return String.join(".", parts).replace(".[", "[");
    }

    private String extractExpectedType(Throwable throwable) {
        Object targetType = invokeReflective(throwable, "getTargetType");
        if (targetType instanceof Class<?> targetClass) {
            return targetClass.getSimpleName();
        }
        Throwable cause = throwable.getCause();
        if (cause != null && cause != throwable) {
            return extractExpectedType(cause);
        }
        return null;
    }

    private Object invokeReflective(Object target, String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
