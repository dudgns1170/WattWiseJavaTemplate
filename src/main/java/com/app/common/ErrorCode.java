package com.app.common;

import org.springframework.http.HttpStatus;

/**
 * 에러 코드 열거형
 * 
 * 애플리케이션에서 발생하는 에러를 정의합니다.
 * 각 에러는 HTTP 상태 코드와 메시지 키를 포함합니다.
 */
public enum ErrorCode {

    // --- 공통 4xx ---
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "invalid_request"),
    AUTHENTICATION_REQUIRED(HttpStatus.UNAUTHORIZED, "authentication_required"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "access_denied"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "resource_not_found"),
    RESOURCE_ALREADY_EXISTS(HttpStatus.CONFLICT, "resource_already_exists"),
    VALIDATION_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "validation_failed"),
    RATE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "rate_limit_exceeded"),

    // --- 공통 5xx ---
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error"),
    BAD_GATEWAY(HttpStatus.BAD_GATEWAY, "bad_gateway"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "service_unavailable"),
    GATEWAY_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "gateway_timeout"),

    // --- 인증/권한 ---
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "invalid_credentials"),
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "user_not_found"),
    PASSWORD_MISMATCH(HttpStatus.UNAUTHORIZED, "password_mismatch"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "token_expired"),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "token_missing"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "invalid_token"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "insufficient_permissions"),

    // --- 클라이언트 플랫폼 헤더 ---
    MISSING_CLIENT_PLATFORM_HEADER(HttpStatus.BAD_REQUEST, "missing_client_platform_header"),
    INVALID_CLIENT_PLATFORM(HttpStatus.BAD_REQUEST, "invalid_client_platform"),

    // --- 사용자 ---
    USER_CREATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "user_create_failed");

    private final HttpStatus httpStatus;
    private final String messageKey;

    ErrorCode(HttpStatus httpStatus, String messageKey) {
        this.httpStatus = httpStatus;
        this.messageKey = messageKey;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessageKey() {
        return messageKey;
    }
}
