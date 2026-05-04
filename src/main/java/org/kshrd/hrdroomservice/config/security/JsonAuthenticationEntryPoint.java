package org.kshrd.hrdroomservice.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import java.util.StringJoiner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        final int status;
        final String errorCode;
        final String message;
        final String wwwAuthenticate;

        if (authException instanceof OAuth2AuthenticationException oae) {
            OAuth2Error error = oae.getError();
            if (error == null) {
                error = new OAuth2Error("invalid_request");
            }
            HttpStatus httpStatus =
                    error instanceof BearerTokenError bte
                            ? bte.getHttpStatus()
                            : HttpStatus.UNAUTHORIZED;
            status = httpStatus.value();
            errorCode = "AUTH_TOKEN_ERROR";
            String description = error.getDescription();
            message =
                    StringUtils.hasText(description)
                            ? description
                            : "Bearer token authentication failed.";
            wwwAuthenticate = buildWwwAuthenticate(error);
            log.warn(
                    "OAuth2 authentication failed for path={} error={}",
                    request.getRequestURI(),
                    Objects.requireNonNullElse(error.getErrorCode(), "unknown"));
        } else if (authException instanceof AuthenticationServiceException) {
            status = HttpStatus.BAD_GATEWAY.value();
            errorCode = "IDP_UNAVAILABLE";
            message = "Identity provider is unavailable.";
            wwwAuthenticate = null;
            log.warn(
                    "Identity provider unavailable for path={}",
                    request.getRequestURI(),
                    authException);
        } else {
            status = HttpStatus.UNAUTHORIZED.value();
            errorCode = "UNAUTHORIZED";
            message = "Authentication is required for this resource.";
            wwwAuthenticate = "Bearer";
            log.warn(
                    "Unauthenticated request for path={}: {}",
                    request.getRequestURI(),
                    Objects.requireNonNullElse(authException.getMessage(), ""));
        }

        if (wwwAuthenticate != null) {
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, wwwAuthenticate);
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiResponse<Void> body =
                ApiResponse.error(status, message, errorCode, request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private static String buildWwwAuthenticate(OAuth2Error error) {
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
}
