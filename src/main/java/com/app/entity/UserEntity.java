package com.app.entity;

import lombok.*;

import java.time.LocalDateTime;

/**
 * 사용자 엔티티
 * 
 * user 테이블과 매핑되는 도메인 객체입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserEntity {
    private String userNo;
    private String userId;
    private String userEmail;
    private String userPw;
    private String userName;
    private String userPhone;
    private String userType;
    private String userAddress;
    private LocalDateTime createdAt; // 생성일
    private LocalDateTime updatedAt; // 수정일
}
