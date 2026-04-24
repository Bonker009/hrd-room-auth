package org.kshrd.hrdroomservice.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    FileUploadResult uploadFile(MultipartFile file, String folderPrefix);

    String previewFile(String key);
}
