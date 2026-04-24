package org.kshrd.hrdroomservice.config.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rustfs")
public class RustFsProperties {

    private boolean enabled;

    private String endpointUrl;

    private String publicEndpointUrl;

    private String bucketName;

    private String accessKeyId;

    private String secretAccessKey;

    private String region = "us-east-1";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpointUrl() {
        return endpointUrl == null ? null : endpointUrl.trim();
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getPublicEndpointUrl() {
        return publicEndpointUrl == null ? null : publicEndpointUrl.trim();
    }

    public void setPublicEndpointUrl(String publicEndpointUrl) {
        this.publicEndpointUrl = publicEndpointUrl;
    }

    public String getBucketName() {
        return bucketName == null ? null : bucketName.trim();
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getAccessKeyId() {
        return accessKeyId == null ? null : accessKeyId.trim();
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey == null ? null : secretAccessKey.trim();
    }

    public void setSecretAccessKey(String secretAccessKey) {
        this.secretAccessKey = secretAccessKey;
    }

    public String getRegion() {
        return region == null ? null : region.trim();
    }

    public void setRegion(String region) {
        this.region = region;
    }
}
