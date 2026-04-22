package org.kshrd.hrdroomservice.config.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.security")
public class AppSecurityProperties {

    /** When false, all requests are permitted (useful for local/tests). */
    private boolean enabled = true;
}
