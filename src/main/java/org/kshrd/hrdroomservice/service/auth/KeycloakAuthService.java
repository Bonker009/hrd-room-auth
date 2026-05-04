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
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.kshrd.hrdroomservice.api.dto.auth.AuthTokenResponse;
import org.kshrd.hrdroomservice.api.dto.auth.LoginRequest;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.security.KeycloakAuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

@Service
@RequiredArgsConstructor
public class KeycloakAuthService implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);
    private static final Pattern KEYCLOAK_OAUTH_ERROR =
            Pattern.compile("\"error\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");
    private static final Pattern KEYCLOAK_OAUTH_ERROR_DESCRIPTION =
            Pattern.compile("\"error_description\"\\s*:\\s*\"([^\"\\\\]*(?:\\\\.[^\"\\\\]*)*)\"");

    private final KeycloakAuthProperties properties;
    private final ObjectMapper objectMapper;

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

    /** Maps Keycloak token-endpoint JSON errors (password grant). */
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
