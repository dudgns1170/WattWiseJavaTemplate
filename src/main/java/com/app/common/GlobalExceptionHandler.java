package com.app.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/**
 * 전역 예외 처리기
 * 
 * 애플리케이션 전체에서 발생하는 예외를 잡아 통일된 응답 형식으로 변환합니다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status;
        ApiResponse body;
        if (errorCode != null) {
            status = errorCode.getHttpStatus();
            body = ApiResponse.error(status, errorCode.getMessageKey());
        } else {
            status = ex.getHttpStatus();
            List<String> messages = new ArrayList<>();
            messages.add(ex.getMessage());
            body = ApiResponse.error(status.value(), messages);
        }
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> messages = new ArrayList<>();
        
        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            String field = fieldError.getField();
            String errorMessage = fieldError.getDefaultMessage();
            messages.add(field + ": " + errorMessage);
        });
        ApiResponse body = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> messages = new ArrayList<>();
        
        ex.getConstraintViolations().forEach(violation -> {
            String path = violation.getPropertyPath().toString();
            String errorMessage = violation.getMessage();
            messages.add(path + ": " + errorMessage);
        });
        ApiResponse body = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), messages);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        ApiResponse body = ApiResponse.error(status, message);
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse> handleBadRequest(RuntimeException ex) {
        ApiResponse body = ApiResponse.error(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRuntimeException(RuntimeException ex) {
        log.error("[RuntimeException] {}", ex.getMessage(), ex);
        ApiResponse body = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception ex) {
        log.error("[Exception] {}", ex.getMessage(), ex);
        ApiResponse body = ApiResponse.error(HttpStatus.INTERNAL_SERVER_ERROR, "internal_server_error");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}
