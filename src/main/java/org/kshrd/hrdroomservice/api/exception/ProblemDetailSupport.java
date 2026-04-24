package org.kshrd.hrdroomservice.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

final class ProblemDetailSupport {

    private static final URI TYPE_BLANK = URI.create("about:blank");

    private ProblemDetailSupport() {}

    static ProblemDetail fromApiException(ApiException ex, HttpServletRequest req) {
        HttpStatusCode statusCode = ex.getStatus();
        String title =
                ex.getProblemTitle() != null
                        ? ex.getProblemTitle()
                        : titleForErrorCode(ex.getErrorCode(), ex.getStatus());
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(statusCode, ex.getMessage());
        pd.setTitle(title);
        pd.setType(TYPE_BLANK);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("errorCode", ex.getErrorCode());
        if (ex.getField() != null) {
            pd.setProperty("field", ex.getField());
        }
        return pd;
    }

    static ProblemDetail fromValidation(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        String detail =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                        .collect(Collectors.joining("; "));
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setTitle("Validation failed");
        pd.setType(TYPE_BLANK);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("errorCode", "VALIDATION_ERROR");
        List<Map<String, String>> violations =
                ex.getBindingResult().getFieldErrors().stream()
                        .map(ProblemDetailSupport::violation)
                        .collect(Collectors.toList());
        pd.setProperty("violations", violations);
        return pd;
    }

    private static Map<String, String> violation(FieldError fe) {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("field", fe.getField());
        m.put("message", fe.getDefaultMessage());
        return m;
    }

    static ProblemDetail simple(
            int status, String title, String detail, String errorCode, HttpServletRequest req) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(status), detail);
        pd.setTitle(title);
        pd.setType(TYPE_BLANK);
        pd.setInstance(URI.create(req.getRequestURI()));
        pd.setProperty("errorCode", errorCode);
        return pd;
    }

    private static String titleForErrorCode(String errorCode, HttpStatus status) {
        if (errorCode == null) {
            return status.getReasonPhrase();
        }
        return switch (errorCode) {
            case "NOT_FOUND" -> "Resource not found";
            case "BAD_REQUEST" -> "Bad request";
            case "CONFLICT" -> "Conflict";
            case "ACCESS_DENIED", "KEYCLOAK_FORBIDDEN" -> "Forbidden";
            case "AUTH_FAILED" -> "Authentication failed";
            case "KEYCLOAK_CLIENT_AUTH" -> "Identity provider configuration error";
            case "UPSTREAM_ERROR" -> "Upstream service error";
            case "UNAUTHORIZED" -> "Unauthorized";
            case "VALIDATION_ERROR" -> "Validation failed";
            case "REGISTRATION_REJECTED" -> "Invalid registration";
            default -> status.getReasonPhrase();
        };
    }
}
