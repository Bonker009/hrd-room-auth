package org.kshrd.hrdroomservice.observability;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.observability")
public record ObservabilityProperties(List<String> trustedProxyCidrs) {

    public ObservabilityProperties {
        if (trustedProxyCidrs == null || trustedProxyCidrs.isEmpty()) {
            trustedProxyCidrs =
                    List.of(
                            "127.0.0.0/8",
                            "::1/128",
                            "10.0.0.0/8",
                            "172.16.0.0/12",
                            "192.168.0.0/16",
                            "fc00::/7");
        }
    }
}
