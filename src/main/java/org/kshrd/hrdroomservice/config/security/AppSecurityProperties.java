package org.kshrd.hrdroomservice.config.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {
 
    private boolean enabled = true;
}
