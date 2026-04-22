package org.kshrd.hrdroomservice.api.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
 
    private final String field;
    private final String problemTitle;

    public ApiException(HttpStatus status, String message, String errorCode) {
        this(status, message, errorCode, null, null);
    }

    public ApiException(HttpStatus status, String message, String errorCode, String field, String problemTitle) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.field = field;
        this.problemTitle = problemTitle;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message, "NOT_FOUND");
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST");
    }

    public static ApiException badRequest(String message, String field, String problemTitle) {
        return new ApiException(HttpStatus.BAD_REQUEST, message, "BAD_REQUEST", field, problemTitle);
    }
 
    public static ApiException registrationRejected(String message, String field) {
        return new ApiException(
                HttpStatus.BAD_REQUEST, message, "REGISTRATION_REJECTED", field, "Invalid registration");
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message, "CONFLICT");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, message, "ACCESS_DENIED");
    }
}
