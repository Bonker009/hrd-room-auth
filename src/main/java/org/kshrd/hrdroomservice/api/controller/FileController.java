package org.kshrd.hrdroomservice.api.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.kshrd.hrdroomservice.api.dto.response.ApiResponse;
import org.kshrd.hrdroomservice.api.dto.response.ResponseUtil;
import org.kshrd.hrdroomservice.api.exception.ApiException;
import org.kshrd.hrdroomservice.storage.FileStorageService;
import org.kshrd.hrdroomservice.storage.FileUploadResult;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v4/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> upload(
            @RequestPart("file") MultipartFile file, @RequestParam(defaultValue = "uploads") String folder) {
        if (file.isEmpty()) {
            throw ApiException.badRequest("File is required");
        }

        FileUploadResult result = fileStorageService.uploadFile(file, folder);
        return ResponseUtil.ok(
                Map.of("url", result.url(), "key", result.key(), "filename", result.filename()), "File uploaded");
    }

    @GetMapping("/preview")
    public ResponseEntity<ApiResponse<Map<String, String>>> preview(@RequestParam String key) {
        if (key.isBlank()) {
            throw ApiException.badRequest("File key is required");
        }
        String url = fileStorageService.previewFile(key);
        return ResponseUtil.ok(Map.of("url", url), "Preview URL generated");
    }
}
