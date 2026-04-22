package org.kshrd.hrdroomservice.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.jwt")
public class AppJwtProperties {

    private String roleClaim = "realm_access.roles";

    private String principalClaim = "sub";
}
