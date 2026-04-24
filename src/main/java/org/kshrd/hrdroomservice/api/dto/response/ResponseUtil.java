package org.kshrd.hrdroomservice.api.dto.response;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public final class ResponseUtil {

    private ResponseUtil() {}

    public static <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok(ApiResponse.success(data, message, HttpStatus.OK.value()));
    }

    public static <T> ResponseEntity<ApiResponse<PageResponse<T>>> okPage(
            PageResponse<T> page, String message) {
        return ResponseEntity.ok(
                ApiResponse.<PageResponse<T>>success(page, message, HttpStatus.OK.value()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> created(T data, String message) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(data, message, HttpStatus.CREATED.value()));
    }

    public static ResponseEntity<ApiResponse<Void>> noContent(String message) {
        return ResponseEntity.status(HttpStatus.NO_CONTENT)
                .body(ApiResponse.success(null, message, HttpStatus.NO_CONTENT.value()));
    }

    public static <T> ResponseEntity<ApiResponse<T>> notFound(String message) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(HttpStatus.NOT_FOUND.value(), message, "NOT_FOUND", null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> badRequest(String message) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ApiResponse.error(
                                HttpStatus.BAD_REQUEST.value(), message, "BAD_REQUEST", null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> conflict(String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(HttpStatus.CONFLICT.value(), message, "CONFLICT", null));
    }

    public static <T> ResponseEntity<ApiResponse<T>> forbidden(String message) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                        ApiResponse.error(
                                HttpStatus.FORBIDDEN.value(), message, "ACCESS_DENIED", null));
    }
}
