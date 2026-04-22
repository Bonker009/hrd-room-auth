package org.kshrd.hrdroomservice.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "keycloak.auth")
public class KeycloakAuthProperties {

    private String authServerUrl;
    private String realm;
    private String clientId;
    private String clientSecret;
}
