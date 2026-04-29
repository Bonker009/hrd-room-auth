package org.kshrd.hrdroomservice.service.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
import org.kshrd.hrdroomservice.api.dto.auth.LogoutRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RefreshTokenRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisterRequest;
import org.kshrd.hrdroomservice.api.dto.auth.RegisteredUserResponse;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.security.KeycloakAuthProperties;
import org.kshrd.hrdroomservice.security.email.DisposableEmailDomainBlocklist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

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
    private final ObjectMapper objectMapper;
    private final DisposableEmailDomainBlocklist disposableEmailDomainBlocklist;

    private KeycloakOAuthHttpClient oauthHttp;

    @PostConstruct
    void initOAuthHttp() {
        oauthHttp =
                new KeycloakOAuthHttpClient(
                        properties, objectMapper, KeycloakAuthService::mapKeycloakTokenFailure);
    }

    @Override
    public AuthTokenResponse login(LoginRequest request) {
        try {
            return oauthHttp.passwordGrant(request.username(), request.password());
        } catch (ApiException ex) {
            log.warn(
                    "Keycloak login failed: realm={} clientId={} username={} status={} code={}",
                    properties.getRealm(),
                    properties.getClientId(),
                    request.username(),
                    ex.getStatus().value(),
                    ex.getErrorCode());
            throw ex;
        } catch (ResourceAccessException ex) {
            log.warn("Keycloak login transport failed: realm={}", properties.getRealm(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "Unable to reach identity provider", "UPSTREAM_ERROR");
        } catch (Exception ex) {
            log.warn("Keycloak login unexpected failure: realm={}", properties.getRealm(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Authentication failed with identity provider",
                    "UPSTREAM_ERROR");
        }
    }

    @Override
    public AuthTokenResponse refresh(RefreshTokenRequest request) {
        try {
            return oauthHttp.refreshGrant(request.refreshToken());
        } catch (ApiException ex) {
            log.warn(
                    "Keycloak token refresh failed: realm={} clientId={} status={} code={}",
                    properties.getRealm(),
                    properties.getClientId(),
                    ex.getStatus().value(),
                    ex.getErrorCode());
            throw ex;
        } catch (ResourceAccessException ex) {
            log.warn("Keycloak refresh transport failed: realm={}", properties.getRealm(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "Unable to reach identity provider", "UPSTREAM_ERROR");
        } catch (Exception ex) {
            log.warn("Keycloak refresh unexpected failure: realm={}", properties.getRealm(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Token refresh failed with identity provider",
                    "UPSTREAM_ERROR");
        }
    }

    @Override
    public void logout(LogoutRequest request) {
        try {
            oauthHttp.logout(request.refreshToken());
        } catch (ApiException ex) {
            throw ex;
        } catch (ResourceAccessException ex) {
            log.warn("Keycloak logout transport failed: realm={}", properties.getRealm(), ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "Unable to reach identity provider", "UPSTREAM_ERROR");
        }
    }

    @Override
    public RegisteredUserResponse register(RegisterRequest request) {
        if (disposableEmailDomainBlocklist.isDisposable(request.email())) {
            throw ApiException.registrationRejected(
                    "Disposable email addresses are not allowed", "email");
        }
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
            user.setEmailVerified(true);
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());

            CredentialRepresentation cred = new CredentialRepresentation();
            cred.setType(CredentialRepresentation.PASSWORD);
            cred.setTemporary(false);
            cred.setValue(request.password());
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
                            request.email(),
                            body);
                    throw mapKeycloakCreateUserFailure(status, body);
                }
                String id = CreatedResponseUtil.getCreatedId(response);
                return new RegisteredUserResponse(
                        id,
                        request.username(),
                        request.email(),
                        request.firstName(),
                        request.lastName());
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (ProcessingException ex) {
            log.warn(
                    "Keycloak register transport failed (cannot reach server): authServerUrl={} realm={}",
                    properties.getAuthServerUrl(),
                    properties.getRealm(),
                    ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to reach identity provider. Check keycloak.auth.auth-server-url and that Keycloak is running.",
                    "UPSTREAM_ERROR");
        } catch (WebApplicationException ex) {
            Response r = ex.getResponse();
            int status = r != null ? r.getStatus() : 0;
            String body = r != null ? readResponseBody(r) : null;
            log.warn(
                    "Keycloak register HTTP error: status={} realm={} clientId={} email={} body={}",
                    status,
                    properties.getRealm(),
                    properties.getClientId(),
                    request.email(),
                    body,
                    ex);
            if (status >= 400 && body != null) {
                throw mapKeycloakCreateUserFailure(status, body);
            }
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Registration failed with identity provider (HTTP " + status + ").",
                    "UPSTREAM_ERROR");
        } catch (IllegalStateException ex) {
            log.warn(
                    "Keycloak register: missing created user id (unexpected 201 response). realm={} email={}",
                    properties.getRealm(),
                    request.email(),
                    ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Registration succeeded but identity provider returned an unexpected response.",
                    "UPSTREAM_ERROR");
        } catch (Exception ex) {
            log.error(
                    "Keycloak register unexpected failure: realm={} clientId={} email={}",
                    properties.getRealm(),
                    properties.getClientId(),
                    request.email(),
                    ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Registration failed with identity provider. See application logs for the underlying error.",
                    "UPSTREAM_ERROR");
        }
    }

    @Override
    public void changeStudentToTeacher(UUID userId) {
        try (Keycloak admin = buildAdminClient()) {
            var realm = admin.realm(properties.getRealm());
            UserResource user = realm.users().get(userId.toString());
            UserRepresentation targetUser = user.toRepresentation();
            if (targetUser == null) {
                throw ApiException.notFound("Student not found");
            }

            RoleRepresentation studentRole = realm.roles().get("student").toRepresentation();
            RoleRepresentation teacherRole = realm.roles().get("teacher").toRepresentation();

            List<RoleRepresentation> assigned = user.roles().realmLevel().listAll();
            boolean hasStudent =
                    assigned.stream()
                            .anyMatch(
                                    role ->
                                            role.getName() != null
                                                    && role.getName().equalsIgnoreCase("student"));
            boolean hasTeacher =
                    assigned.stream()
                            .anyMatch(
                                    role ->
                                            role.getName() != null
                                                    && role.getName().equalsIgnoreCase("teacher"));

            if (hasStudent) {
                user.roles().realmLevel().remove(List.of(studentRole));
            }
            if (!hasTeacher) {
                user.roles().realmLevel().add(List.of(teacherRole));
            }
        } catch (ApiException ex) {
            throw ex;
        } catch (WebApplicationException ex) {
            throw mapRoleChangeWebException(ex, userId);
        } catch (ProcessingException ex) {
            log.warn(
                    "Keycloak role-change transport failed: realm={} userId={}",
                    properties.getRealm(),
                    userId,
                    ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY, "Unable to reach identity provider", "UPSTREAM_ERROR");
        } catch (Exception ex) {
            log.error(
                    "Keycloak role-change unexpected failure: realm={} userId={}",
                    properties.getRealm(),
                    userId,
                    ex);
            throw new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Role update failed with identity provider",
                    "UPSTREAM_ERROR");
        }
    }

    Keycloak buildAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(trimTrailingSlash(properties.getAuthServerUrl()))
                .realm(properties.getRealm())
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .build();
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

    /** Maps Keycloak token-endpoint JSON errors (password grant, refresh grant). */
    private static ApiException mapKeycloakTokenFailure(int status, String body) {
        String error = extractJsonValue(body, KEYCLOAK_OAUTH_ERROR);
        String description = extractJsonValue(body, KEYCLOAK_OAUTH_ERROR_DESCRIPTION);
        if ("invalid_grant".equals(error)) {
            if (description != null
                    && description.toLowerCase().contains("invalid user credentials")) {
                return new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Invalid username or password",
                        "AUTH_FAILED",
                        "username",
                        "Authentication failed");
            }
            if (description != null
                    && (description.toLowerCase().contains("invalid refresh token")
                            || description.toLowerCase().contains("token is not active")
                            || description.toLowerCase().contains("session not active"))) {
                return new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Refresh token is invalid or expired",
                        "AUTH_REFRESH_INVALID",
                        null,
                        "Authentication failed");
            }
            if (description != null
                    && description.toLowerCase().contains("account is not fully set up")) {
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
                    "This Keycloak client is not allowed to use this grant type. Check client configuration.",
                    "KEYCLOAK_CLIENT_POLICY",
                    null,
                    "Identity provider configuration error");
        }
        if ("invalid_client".equals(error)) {
            return new ApiException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak rejected the client credentials. Check keycloak.auth client-id and client-secret.",
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
            return new ApiException(
                    HttpStatus.UNAUTHORIZED, "Invalid username or password", "AUTH_FAILED");
        }
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "Token request failed with identity provider (HTTP " + status + ").",
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

    private ApiException mapRoleChangeWebException(WebApplicationException ex, UUID userId) {
        Response response = ex.getResponse();
        int status = response != null ? response.getStatus() : 0;
        String body = response != null ? readResponseBody(response) : null;
        log.warn(
                "Keycloak role-change HTTP error: status={} realm={} userId={} body={}",
                status,
                properties.getRealm(),
                userId,
                body,
                ex);
        if (status == 403) {
            return new ApiException(
                    HttpStatus.FORBIDDEN,
                    "Keycloak denied role update for this client. Ensure realm-management roles include manage-users and manage-realm.",
                    "KEYCLOAK_FORBIDDEN");
        }
        if (status == 404) {
            return ApiException.notFound("User or role was not found in identity provider");
        }
        return new ApiException(
                HttpStatus.BAD_GATEWAY,
                "Role update failed with identity provider (HTTP " + status + ").",
                "UPSTREAM_ERROR");
    }
}
