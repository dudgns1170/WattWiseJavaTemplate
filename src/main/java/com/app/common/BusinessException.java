package com.app.common;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 예외 클래스
 * 
 * 비즈니스 규칙 위반 시 발생하는 예외입니다.
 * GlobalExceptionHandler에서 적절한 HTTP 응답으로 변환됩니다.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final ErrorCode errorCode;

    public BusinessException(HttpStatus httpStatus, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = null;
    }

    public BusinessException(HttpStatus httpStatus, String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = null;
    }

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessageKey());
        this.httpStatus = errorCode.getHttpStatus();
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessageKey(), cause);
        this.httpStatus = errorCode.getHttpStatus();
        this.errorCode = errorCode;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public int getCode() {
        return httpStatus.value();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
