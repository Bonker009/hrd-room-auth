package org.kshrd.hrdroomservice.service.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.keycloak.OAuth2Constants;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.security.KeycloakAuthProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Timeout-aware HTTP calls to Keycloak OIDC token and logout endpoints (password grant, refresh, logout).
 */
final class KeycloakOAuthHttpClient {

    private final KeycloakAuthProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final TokenEndpointFailureMapper failureMapper;

    KeycloakOAuthHttpClient(
            KeycloakAuthProperties properties,
            ObjectMapper objectMapper,
            TokenEndpointFailureMapper failureMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.failureMapper = failureMapper;
        java.time.Duration connect =
                properties.getHttpConnectTimeout() != null
                        ? properties.getHttpConnectTimeout()
                        : java.time.Duration.ofSeconds(5);
        java.time.Duration read =
                properties.getHttpReadTimeout() != null
                        ? properties.getHttpReadTimeout()
                        : java.time.Duration.ofSeconds(30);
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connect.toMillis());
        factory.setReadTimeout((int) read.toMillis());
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    AuthTokenResponse passwordGrant(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
        form.add(OAuth2Constants.USERNAME, username);
        form.add(OAuth2Constants.PASSWORD, password);
        addClientCredentials(form);
        return postToken(form);
    }

    AuthTokenResponse refreshGrant(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        form.add(OAuth2Constants.REFRESH_TOKEN, refreshToken);
        addClientCredentials(form);
        return postToken(form);
    }

    void logout(String refreshToken) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.getClientId());
        if (hasClientSecret()) {
            form.add("client_secret", properties.getClientSecret());
        }
        form.add(OAuth2Constants.REFRESH_TOKEN, refreshToken);
        try {
            restClient
                    .post()
                    .uri(logoutUrl())
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Logout failed", "AUTH_FAILED");
        }
    }

    private void addClientCredentials(MultiValueMap<String, String> form) {
        form.add(OAuth2Constants.CLIENT_ID, properties.getClientId());
        if (hasClientSecret()) {
            form.add(OAuth2Constants.CLIENT_SECRET, properties.getClientSecret());
        }
    }

    private boolean hasClientSecret() {
        return properties.getClientSecret() != null && !properties.getClientSecret().isBlank();
    }

    private AuthTokenResponse postToken(MultiValueMap<String, String> form) {
        try {
            String json =
                    restClient
                            .post()
                            .uri(tokenUrl())
                            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                            .body(form)
                            .retrieve()
                            .body(String.class);
            return parseTokenResponse(json);
        } catch (RestClientResponseException ex) {
            throw failureMapper.map(ex.getStatusCode().value(), ex.getResponseBodyAsString());
        }
    }

    private AuthTokenResponse parseTokenResponse(String json) {
        try {
            JsonNode n = objectMapper.readTree(json);
            if (n.has("error")) {
                throw failureMapper.map(400, json);
            }
            String refresh = n.hasNonNull("refresh_token") ? n.get("refresh_token").asText() : null;
            long refreshExpires =
                    n.has("refresh_expires_in") && n.get("refresh_expires_in").isNumber()
                            ? n.get("refresh_expires_in").asLong()
                            : 0L;
            return new AuthTokenResponse(
                    n.get("access_token").asText(),
                    refresh,
                    n.get("expires_in").asLong(),
                    refreshExpires,
                    n.path("token_type").asText("Bearer"),
                    n.path("scope").isMissingNode() || n.get("scope").isNull()
                            ? null
                            : n.get("scope").asText());
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Unexpected token response from identity provider",
                    "UPSTREAM_ERROR");
        }
    }

    private String tokenUrl() {
        return trimTrailingSlash(properties.getAuthServerUrl())
                + "/realms/"
                + properties.getRealm()
                + "/protocol/openid-connect/token";
    }

    private String logoutUrl() {
        return trimTrailingSlash(properties.getAuthServerUrl())
                + "/realms/"
                + properties.getRealm()
                + "/protocol/openid-connect/logout";
    }

    private static String trimTrailingSlash(String url) {
        if (url == null) {
            return "";
        }
        return url.replaceAll("/+$", "");
    }

    @FunctionalInterface
    interface TokenEndpointFailureMapper {
        ApiException map(int httpStatus, String responseBody);
    }
}
