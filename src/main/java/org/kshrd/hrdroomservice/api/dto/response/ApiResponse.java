package org.kshrd.hrdroomservice.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        int statusCode,
        String message,
        T data,
        Instant timestamp,
        String errorCode,
        String path,
        Map<String, Object> details) {

    public static <T> ApiResponse<T> success(T data, String message, int statusCode) {
        return new ApiResponse<>(true, statusCode, message, data, Instant.now(), null, null, null);
    }

    public static <T> ApiResponse<T> error(
            int statusCode, String message, String errorCode, String path) {
        return new ApiResponse<>(
                false, statusCode, message, null, Instant.now(), errorCode, path, null);
    }

    public static <T> ApiResponse<T> error(
            int statusCode,
            String message,
            String errorCode,
            String path,
            Map<String, Object> details) {
        return new ApiResponse<>(
                false, statusCode, message, null, Instant.now(), errorCode, path, details);
    }
}
