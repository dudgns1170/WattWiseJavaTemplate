package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 로그인 요청 DTO
 * 
 * 클라이언트에서 로그인 시 전송하는 데이터를 담는 객체입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequestDto {
    
    /**
     * 사용자 ID (로그인 아이디)
     * 예: "user123"
     */
    private String userId;
    
    /**
     * 비밀번호 (평문)
     * 서버에서 BCrypt로 해싱된 값과 비교합니다.
     * 예: "password123"
     */
    private String password;
}
