package org.kshrd.hrdroomservice.service.auth;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
import org.kshrd.hrdroomservice.api.dto.auth.LogoutRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisterRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisteredUserResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.security.KeycloakAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KeycloakAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);
    private static final Pattern KEYCLOAK_ERROR_MESSAGE =
            Pattern.compile("\"errorMessage\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private static final Pattern KEYCLOAK_FIELD =
            Pattern.compile("\"field\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private static final Pattern KEYCLOAK_OAUTH_ERROR =
            Pattern.compile("\"error\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private static final Pattern KEYCLOAK_OAUTH_ERROR_DESCRIPTION =
            Pattern.compile("\"error_description\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private final KeycloakAuthProperties properties;

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        try (Keycloak keycloak =
                     KeycloakBuilder.builder()
                             .serverUrl(trimTrailingSlash(properties.getAuthServerUrl()))
                             .realm(properties.getRealm())
                             .grantType(OAuth2Constants.PASSWORD)
                             .username(request.getUsername())
                             .password(request.getPassword())
                             .clientId(properties.getClientId())
                             .clientSecret(properties.getClientSecret())
                             .build()) {
            AccessTokenResponse token = keycloak.tokenManager().getAccessToken();
            return AuthTokenResponse.builder()
                    .accessToken(token.getToken())
                    .refreshToken(token.getRefreshToken())
                    .expiresIn(token.getExpiresIn())
                    .refreshExpiresIn(token.getRefreshExpiresIn())
                    .tokenType(token.getTokenType())
                    .scope(token.getScope())
                    .build();
        } catch (WebApplicationException ex) {
            Response response = ex.getResponse();
            int status = response == null ? 0 : response.getStatus();
            String body = readResponseBody(response);
            log.warn(
                    "Keycloak login failed: status={} realm={} clientId={} username={} body={}",
                    status,
                    properties.getRealm(),
                    properties.getClientId(),
                    request.getUsername(),
                    body);
            throw mapKeycloakLoginFailure(status, body);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password", "AUTH_FAILED");
        }
    }

    @Override
    public void logout(LogoutRequest request) {
        Form form = new Form();
        form.param("client_id", properties.getClientId());
        if (properties.getClientSecret() != null && !properties.getClientSecret().isBlank()) {
            form.param("client_secret", properties.getClientSecret());
        }
        form.param("refresh_token", request.getRefreshToken());

        try (Client client = ClientBuilder.newClient()) {
            try (Response response =
                         client.target(logoutUrl())
                                 .request(MediaType.APPLICATION_JSON_TYPE)
                                 .post(Entity.form(form))) {
                int status = response.getStatus();
                if (status >= 200 && status < 300) {
                    return;
                }
                throw new ApiException(HttpStatus.UNAUTHORIZED, "Logout failed", "AUTH_FAILED");
            }
        }
    }

    @Override
    public RegisteredUserResponse register(RegisterRequest request) {
        try (Keycloak admin =
                     KeycloakBuilder.builder()
                             .serverUrl(trimTrailingSlash(properties.getAuthServerUrl()))
                             .realm(properties.getRealm())
                             .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                             .clientId(properties.getClientId())
                             .clientSecret(properties.getClientSecret())
                             .build()) {

            UserRepresentation user = new UserRepresentation();
            user.setEnabled(true);
            user.setUsername(request.getUsername());
            user.setEmail(request.getEmail());
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());

            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setTemporary(false);
            cred.setValue(request.getPassword());
            user.setCredentials(java.util.List.of(cred));

            try (Response response = admin.realm(properties.getRealm()).users().create(user)) {
                int status = response.getStatus();
                if (status == 409) {
                    throw ApiException.conflict("User already exists");
                }
                if (status >= 400) {
                    String body = readResponseBody(response);
                    log.warn(
                            "Keycloak create user failed: status={} realm={} clientId={} email={} body={}",
                            status,
                            properties.getRealm(),
                            properties.getClientId(),
                            request.getEmail(),
                            body);
                    throw mapKeycloakCreateUserFailure(status, body);
                }
                String id = CreatedResponseUtil.getCreatedId(response);
                return RegisteredUserResponse.builder()
                        .userId(id)
                        .username(request.getUsername())
                        .email(request.getEmail())
                        .firstName(request.getFirstName())
                        .lastName(request.getLastName())
                        .build();
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Registration failed with identity provider",
                    "UPSTREAM_ERROR");
        }
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

    private static String readResponseBody(Response response) {
        if (response == null || !response.hasEntity()) {
            return null;
        }
        try {
            return response.readEntity(String.class);
        } catch (Exception ex) {
            return null;
        }
    }

    private static String extractKeycloakErrorMessage(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher m = KEYCLOAK_ERROR_MESSAGE.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static String extractJsonValue(String json, Pattern pattern) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher m = pattern.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }

    private static ApiException mapKeycloakLoginFailure(int status, String body) {
        String error = extractJsonValue(body, KEYCLOAK_OAUTH_ERROR);
        String description = extractJsonValue(body, KEYCLOAK_OAUTH_ERROR_DESCRIPTION);
        if ("invalid_grant".equals(error)) {
            if (description != null && description.toLowerCase().contains("invalid user credentials")) {
                return new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password",
                        "AUTH_FAILED",
                        "username",
                        "Authentication failed");
            }
            if (description != null && description.toLowerCase().contains("account is not fully set up")) {
                return new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Your account is not fully set up in Keycloak. Please complete required actions and try again.",
                        "AUTH_ACCOUNT_SETUP",
                        null,
                        "Authentication failed");
            }
        }
        if ("unauthorized_client".equals(error)) {
            return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "This Keycloak client is not allowed to use password login. Enable Direct Access Grants for the client.",
                    "KEYCLOAK_CLIENT_POLICY",
                    null,
                    "Identity provider configuration error");
        }
        if ("invalid_client".equals(error)) {
            return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak rejected the client credentials for login. Check keycloak.auth client-id and client-secret.",
                    "KEYCLOAK_CLIENT_AUTH",
                    null,
                    "Identity provider configuration error");
        }
        if (description != null && !description.isBlank()) {
            return new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    description,
                    "AUTH_FAILED",
                    null,
                    "Authentication failed");
        }
        if (status == 401) {
            return new ApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password", "AUTH_FAILED");
        }
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "Login failed with identity provider (HTTP " + status + ").",
                "UPSTREAM_ERROR",
                null,
                "Authentication failed");
    }

    private static ApiException mapKeycloakCreateUserFailure(int status, String body) {
        String keycloakMsg = extractKeycloakErrorMessage(body);
        if (status == 401) {
            return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak rejected the client credentials (check keycloak.auth client-id and client-secret).",
                    "KEYCLOAK_CLIENT_AUTH");
        }
        if (status == 403) {
            return new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Keycloak denied creating users for this client. In the Keycloak admin console: enable "
                            + "service accounts on the client, then assign realm-management roles to the service "
                            + "account (typically manage-users, query-users, view-users).",
                    "KEYCLOAK_FORBIDDEN");
        }
        if (status == 400) {
            String field = extractKeycloakField(body);
            String detail =
                    keycloakMsg != null
                            ? KeycloakErrorMessageMapper.toUserMessage(keycloakMsg)
                            : "Registration was rejected by the identity provider.";
            return ApiException.registrationRejected(detail, field);
        }
        if (keycloakMsg != null) {
            String field = extractKeycloakField(body);
            return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    KeycloakErrorMessageMapper.toUserMessage(keycloakMsg),
                    "UPSTREAM_ERROR",
                    field,
                    "Registration failed");
        }
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "Registration failed with identity provider (HTTP " + status + ").",
                "UPSTREAM_ERROR",
                extractKeycloakField(body),
                "Registration failed");
    }

    private static String extractKeycloakField(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        Matcher m = KEYCLOAK_FIELD.matcher(json);
        if (!m.find()) {
            return null;
        }
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\");
    }
}
