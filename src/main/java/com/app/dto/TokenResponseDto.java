package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 응답 DTO
 * 
 * 로그인 또는 토큰 갱신 성공 시 클라이언트에게 반환하는 토큰 정보입니다.
 * Access Token과 Refresh Token 쌍을 포함합니다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TokenResponseDto {
    
    /**
     * Access Token (JWT)
     * 
     * 실제 API 요청 시 사용하는 단기 토큰입니다.
     * - 유효 기간: application.yml의 app.jwt.access-ttl-minutes 설정 값
     * - 사용 방법: Authorization: Bearer {accessToken}
     * 
     * 예: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwiaWF0IjoxNjE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
     */
    private String accessToken;
    
    /**
     * Refresh Token (JWT)
     * 
     * Access Token 재발급을 위한 장기 토큰입니다.
     * - 유효 기간: application.yml의 app.jwt.refresh-ttl-days 설정 값
     * - Redis에 저장되어 서버 측에서도 관리됨
     * - 탈취 감지 시 즉시 무효화 가능
     * 
     * 참고: 웹(web) 클라이언트는 Refresh Token을 HttpOnly 쿠키로만 전달/회전하도록 설계되어
     *       응답 바디에서는 refreshToken이 null일 수 있습니다.
     * 
     * 예: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIiwidHlwIjoicnQiLCJmaWQiOiJmYW1pbHktdXVpZCIsImp0aSI6InRva2VuLXV1aWQifQ..."
     */
    private String refreshToken;
}
