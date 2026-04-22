package org.kshrd.hrdroomservice.config.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "rustfs")
public class RustFsProperties {

    private boolean enabled;

    private String endpointUrl;

    private String bucketName;

    private String accessKeyId;

    private String secretAccessKey;

    private String region = "us-east-1";
}
