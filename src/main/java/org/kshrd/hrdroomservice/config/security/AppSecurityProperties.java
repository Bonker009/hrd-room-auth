package org.kshrd.hrdroomservice.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    private boolean enabled = true;
}
