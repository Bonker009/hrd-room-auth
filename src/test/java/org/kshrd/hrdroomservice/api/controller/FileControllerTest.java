package org.kshrd.hrdroomservice.api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kshrd.hrdroomservice.api.exception.GlobalExceptionHandler;
import org.kshrd.hrdroomservice.storage.FileStorageService;
import org.kshrd.hrdroomservice.storage.FileUploadResult;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private FileStorageService fileStorageService;

    private MockMvc mockMvc() {
        return MockMvcBuilders.standaloneSetup(new FileController(fileStorageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void upload_returnsWrappedUrlResponse() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "avatar.png", "image/png", "image-bytes".getBytes());
        when(fileStorageService.uploadFile(any(), eq("classrooms")))
                .thenReturn(
                        new FileUploadResult(
                                "classrooms/486642a2-18a4-401f-8882-cceef1f7f383.jpg",
                                "https://files.local/avatar.png",
                                "avatar.png"));

        mockMvc()
                .perform(multipart("/api/v4/files/upload").file(file).param("folder", "classrooms"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.url").value("https://files.local/avatar.png"))
                .andExpect(
                        jsonPath("$.data.key")
                                .value("classrooms/486642a2-18a4-401f-8882-cceef1f7f383.jpg"))
                .andExpect(jsonPath("$.data.filename").value("avatar.png"));

        verify(fileStorageService).uploadFile(any(), eq("classrooms"));
    }

    @Test
    void upload_emptyFile_returnsBadRequest() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "empty.png", "image/png", new byte[0]);

        mockMvc()
                .perform(multipart("/api/v4/files/upload").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("File is required"))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }

    @Test
    void upload_withoutAuthentication_stillWorksInStandaloneMvcPattern() throws Exception {
        MockMultipartFile file =
                new MockMultipartFile("file", "avatar.png", "image/png", "image-bytes".getBytes());
        when(fileStorageService.uploadFile(any(), eq("uploads")))
                .thenReturn(
                        new FileUploadResult(
                                "uploads/486642a2-18a4-401f-8882-cceef1f7f383.jpg",
                                "https://files.local/default.png",
                                "avatar.png"));

        mockMvc()
                .perform(multipart("/api/v4/files/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://files.local/default.png"));
    }

    @Test
    void preview_returnsWrappedUrlResponse() throws Exception {
        when(fileStorageService.previewFile("classrooms/a.png"))
                .thenReturn("https://files.local/bucket/classrooms/a.png");

        mockMvc()
                .perform(get("/api/v4/files/preview").param("key", "classrooms/a.png"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(
                        jsonPath("$.data.url")
                                .value("https://files.local/bucket/classrooms/a.png"));

        verify(fileStorageService).previewFile("classrooms/a.png");
    }

    @Test
    void preview_blankKey_returnsBadRequest() throws Exception {
        mockMvc()
                .perform(get("/api/v4/files/preview").param("key", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("File key is required"))
                .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"));
    }
}
