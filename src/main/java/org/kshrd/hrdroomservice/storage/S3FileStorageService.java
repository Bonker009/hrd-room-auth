package org.kshrd.hrdroomservice.storage;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.storage.RustFsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rustfs", name = "enabled", havingValue = "true")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final RustFsProperties rustFsProperties;

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folderPrefix) {
        String safePrefix =
                folderPrefix == null ? "uploads" : folderPrefix.replaceAll("^/+|/+$", "");
        String ext = extensionOf(file.getOriginalFilename(), file.getContentType());
        String key = safePrefix + "/" + UUID.randomUUID() + ext;
        try {
            PutObjectRequest req =
                    PutObjectRequest.builder()
                            .bucket(rustFsProperties.getBucketName())
                            .key(key)
                            .contentType(file.getContentType())
                            .build();
            s3Client.putObject(req, RequestBody.fromBytes(file.getBytes()));
            String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
            return new FileUploadResult(key, buildObjectUrl(key), filename);
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to read uploaded file");
        }
    }

    @Override
    public String previewFile(String key) {
        if (key == null || key.isBlank()) {
            throw ApiException.badRequest("File key is required");
        }
        String normalized = key.trim();
        // Always presign: callers often pass the public object URL from upload, which is not signed
        // and would return AccessDenied if returned as-is.
        String safeKey =
                normalized.startsWith("http://") || normalized.startsWith("https://")
                        ? extractObjectKey(normalized)
                        : normalized.replaceAll("^/+", "");
        if (safeKey.isBlank()) {
            throw ApiException.badRequest("File key is required");
        }
        String bucket = rustFsProperties.getBucketName();
        if (bucket == null || bucket.isBlank()) {
            throw ApiException.badRequest("Storage bucket is not configured");
        }
        GetObjectRequest getObjectRequest =
                GetObjectRequest.builder().bucket(bucket).key(safeKey).build();
        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(15))
                        .getObjectRequest(getObjectRequest)
                        .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String buildObjectUrl(String key) {
        String publicBase = rustFsProperties.getPublicEndpointUrl();
        String resolvedBase =
                (publicBase == null || publicBase.isBlank())
                        ? rustFsProperties.getEndpointUrl()
                        : publicBase;
        String base = resolvedBase.replaceAll("/+$", "");
        return base + "/" + rustFsProperties.getBucketName() + "/" + key;
    }

    private String extractObjectKey(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            URI uri = URI.create(value);
            String rawPath = uri.getPath() == null ? "" : uri.getPath().replaceAll("^/+", "");
            String path;
            try {
                path = URLDecoder.decode(rawPath, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                path = rawPath;
            }
            String bucketPrefix = rustFsProperties.getBucketName() + "/";
            if (path.startsWith(bucketPrefix)) {
                return path.substring(bucketPrefix.length());
            }
            return path;
        }
        return value.replaceAll("^/+", "");
    }

    private static String extensionOf(String filename, String contentType) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf('.'));
        }
        if (contentType != null && contentType.contains("/")) {
            return "." + contentType.substring(contentType.indexOf('/') + 1);
        }
        return "";
    }
}
