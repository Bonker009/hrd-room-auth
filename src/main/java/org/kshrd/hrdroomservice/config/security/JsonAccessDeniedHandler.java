package org.kshrd.hrdroomservice.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final String INSUFFICIENT_SCOPE_WWW_AUTHENTICATE =
            "Bearer error=\"insufficient_scope\", "
                    + "error_description=\"The request requires higher privileges than provided by the access token.\", "
                    + "error_uri=\"https://tools.ietf.org/html/rfc6750#section-3.1\"";

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        response.setStatus(403);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        if (request.getUserPrincipal() instanceof AbstractOAuth2TokenAuthenticationToken<?>) {
            response.addHeader(HttpHeaders.WWW_AUTHENTICATE, INSUFFICIENT_SCOPE_WWW_AUTHENTICATE);
        }
        ApiResponse<Void> body =
                ApiResponse.error(403, "Forbidden", "ACCESS_DENIED", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
