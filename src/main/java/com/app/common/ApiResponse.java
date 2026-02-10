package com.app.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {

    /**
     * 비즈니스 성공 여부
     * true: 성공, false: 실패
     */
    private boolean status;

    /**
     * HTTP 상태 코드 (예: 200, 400, 401 ...)
     */
    private int code;

    /**
     * 메시지 (성공/에러 메시지). 값이 없으면 null.
     */
    private String message;

    /**
     * 성공 시 응답 데이터. 값이 없으면 null.
     */
    private Object data;

    private static ApiResponse of(boolean status, int code, String message, Object data) {
        return ApiResponse.builder()
                .status(status)
                .code(code)
                .message(message == null || message.isBlank() ? null : message)
                .data(data)
                .build();
    }

    /**
     * 성공 응답 (메시지 없이 데이터만 반환)
     */
    public static ApiResponse success(int statusCode, Object data) {
        return of(true, statusCode, null, data);
    }

    /**
     * 성공 응답 (단일 메시지 + 데이터)
     */
    public static ApiResponse success(int statusCode, String message, Object data) {
        return of(true, statusCode, message, data);
    }

    public static ApiResponse success(HttpStatus status, Object data) {
        return success(status.value(), data);
    }

    public static ApiResponse success(HttpStatus status, String message, Object data) {
        return success(status.value(), message, data);
    }

    /**
     * 200 OK 성공 응답 (기본 message: "success")
     */
    public static ApiResponse ok(Object data) {
        return of(true, HttpStatus.OK.value(), "success", data);
    }

    /**
     * 201 Created 성공 응답 (기본 message: "created")
     */
    public static ApiResponse created(Object data) {
        return of(true, HttpStatus.CREATED.value(), "created", data);
    }

    /**
     * 에러 응답 (단일 메시지)
     */
    public static ApiResponse error(int statusCode, String message) {
        return of(false, statusCode, message, null);
    }

    public static ApiResponse error(HttpStatus status, String message) {
        return error(status.value(), message);
    }

    /**
     * 에러 응답 (여러 메시지)
     */
    public static ApiResponse error(int statusCode, List<String> messages) {
        String joined = (messages == null || messages.isEmpty())
                ? null
                : String.join(", ", messages);
        return of(false, statusCode, joined, null);
    }
}
