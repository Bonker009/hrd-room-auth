package org.kshrd.hrdroomservice.storage;

import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(
        prefix = "rustfs",
        name = "enabled",
        havingValue = "false",
        matchIfMissing = true)
public class DisabledFileStorageService implements FileStorageService {

    @Override
    public FileUploadResult uploadFile(MultipartFile file, String folderPrefix) {
        throw ApiException.badRequest(
                "Object storage (RustFS) is disabled. Enable rustfs.enabled and configure credentials.");
    }

    @Override
    public String previewFile(String key) {
        throw ApiException.badRequest(
                "Object storage (RustFS) is disabled. Enable rustfs.enabled and configure credentials.");
    }
}
