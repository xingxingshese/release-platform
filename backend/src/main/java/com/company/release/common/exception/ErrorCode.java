package com.company.release.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 统一错误码（规范 §28）。
 */
public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    BUSINESS_ERROR(HttpStatus.CONFLICT),
    AUTH_ERROR(HttpStatus.UNAUTHORIZED),
    PERMISSION_DENIED(HttpStatus.FORBIDDEN),
    NOT_FOUND(HttpStatus.NOT_FOUND),
    EXTERNAL_SERVICE_ERROR(HttpStatus.BAD_GATEWAY),
    TIMEOUT(HttpStatus.GATEWAY_TIMEOUT),
    CONFLICT(HttpStatus.CONFLICT),
    IDEMPOTENCY_ERROR(HttpStatus.CONFLICT),
    SYSTEM_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
