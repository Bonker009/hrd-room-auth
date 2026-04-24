package org.kshrd.hrdroomservice.config.storage;

import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rustfs", name = "enabled", havingValue = "true")
public class S3Config {

    private final RustFsProperties rustFsProperties;

    @Bean
    S3Client s3Client() {
        if (rustFsProperties.getEndpointUrl() == null
                || rustFsProperties.getEndpointUrl().isBlank()) {
            throw new IllegalStateException(
                    "rustfs.endpoint-url is required when rustfs.enabled=true");
        }
        if (rustFsProperties.getAccessKeyId() == null
                || rustFsProperties.getSecretAccessKey() == null) {
            throw new IllegalStateException(
                    "RustFS access key and secret are required when rustfs.enabled=true");
        }
        if (rustFsProperties.getBucketName() == null
                || rustFsProperties.getBucketName().isBlank()) {
            throw new IllegalStateException(
                    "rustfs.bucket-name is required when rustfs.enabled=true");
        }
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        rustFsProperties.getAccessKeyId(), rustFsProperties.getSecretAccessKey());
        S3Configuration s3Configuration =
                S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        // Browser-friendly presigned GETs with third-party S3 endpoints
                        // (RustFS/MinIO)
                        .checksumValidationEnabled(false)
                        .build();
        return S3Client.builder()
                .endpointOverride(URI.create(rustFsProperties.getEndpointUrl()))
                .region(Region.of(rustFsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration)
                .build();
    }

    @Bean
    S3Presigner s3Presigner() {
        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(
                        rustFsProperties.getAccessKeyId(), rustFsProperties.getSecretAccessKey());
        S3Configuration s3Configuration =
                S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .checksumValidationEnabled(false)
                        .build();
        String signingEndpoint =
                rustFsProperties.getPublicEndpointUrl() == null
                                || rustFsProperties.getPublicEndpointUrl().isBlank()
                        ? rustFsProperties.getEndpointUrl()
                        : rustFsProperties.getPublicEndpointUrl();
        return S3Presigner.builder()
                .endpointOverride(URI.create(signingEndpoint))
                .region(Region.of(rustFsProperties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .serviceConfiguration(s3Configuration)
                .build();
    }
}
