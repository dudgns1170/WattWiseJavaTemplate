package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Refresh Token 갱신 요청 DTO
 * 
 * Access Token이 만료되었을 때, Refresh Token을 사용하여
 * 새로운 토큰 쌍을 발급받기 위한 요청 데이터입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RefreshRequestDto {
    
    /**
     * Refresh Token (JWT 문자열)
     * 로그인 시 발급받은 Refresh Token을 그대로 전송합니다.
     * 예: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     */
    private String refreshToken;
}
