package org.kshrd.hrdroomservice.config.security;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "keycloak.auth")
public class KeycloakAuthProperties {

    private String authServerUrl;
    private String realm;
    private String clientId;
    private String clientSecret;

    private Duration httpConnectTimeout = Duration.ofSeconds(5);

    private Duration httpReadTimeout = Duration.ofSeconds(30);
}
