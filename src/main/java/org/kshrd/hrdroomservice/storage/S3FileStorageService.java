package org.kshrd.hrdroomservice.storage;

import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.config.storage.RustFsProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rustfs", name = "enabled", havingValue = "true")
public class S3FileStorageService implements FileStorageService {

    private final S3Client s3Client;
    private final RustFsProperties rustFsProperties;

    @Override
    public String uploadFile(MultipartFile file, String folderPrefix) {
        String safePrefix = folderPrefix == null ? "uploads" : folderPrefix.replaceAll("^/+|/+$", "");
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
            String base = rustFsProperties.getEndpointUrl().replaceAll("/+$", "");
            return base + "/" + rustFsProperties.getBucketName() + "/" + key;
        } catch (IOException e) {
            throw ApiException.badRequest("Failed to read uploaded file");
        }
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
