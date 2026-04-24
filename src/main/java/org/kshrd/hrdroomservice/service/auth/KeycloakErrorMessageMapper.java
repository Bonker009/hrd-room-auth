package org.kshrd.hrdroomservice.service.auth;

import java.util.Locale;

/** Maps Keycloak Admin API {@code errorMessage} keys to user-facing text. */
public final class KeycloakErrorMessageMapper {

    private KeycloakErrorMessageMapper() {}

    /**
     * @param keycloakErrorMessage value from Keycloak JSON {@code errorMessage} (often {@code
     *     error-…})
     */
    public static String toUserMessage(String keycloakErrorMessage) {
        if (keycloakErrorMessage == null || keycloakErrorMessage.isBlank()) {
            return "The identity provider rejected this request.";
        }
        String key = keycloakErrorMessage.trim();

        return switch (key) {
            case "error-username-invalid-character" ->
                    "Username contains characters that are not allowed for this realm. "
                            + "Use only letters, digits, and symbols your administrator allows "
                            + "(many realms disallow spaces or '@' in usernames).";
            case "error-duplicate-username" -> "That username is already taken.";
            case "error-duplicate-email" -> "That email address is already registered.";
            case "error-user-attribute-required" ->
                    "A required user attribute is missing. Check username, email, and name fields.";
            case "error-invalid-value" ->
                    "One of the submitted values is not valid for this realm.";
            case "error-password-rejected" ->
                    "Password does not meet the realm password policy. Choose a stronger or longer password.";
            case "error-password-too-short" -> "Password is too short for this realm.";
            case "error-password-too-young" ->
                    "Password cannot be reused yet (password history policy).";
            case "error-person-name-invalid-character" ->
                    "First or last name contains characters that are not allowed.";
            case "error-email-invalid" -> "Email format is not accepted by the identity provider.";
            default -> humanizeUnknown(key);
        };
    }

    private static String humanizeUnknown(String key) {
        if (key.startsWith("error-")) {
            String rest = key.substring("error-".length()).replace('-', ' ');
            return "Registration was rejected: " + capitalizeWords(rest) + ".";
        }
        return key;
    }

    private static String capitalizeWords(String rest) {
        String[] parts = rest.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(p.substring(0, 1).toUpperCase(Locale.ROOT));
            if (p.length() > 1) {
                sb.append(p.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }
}
