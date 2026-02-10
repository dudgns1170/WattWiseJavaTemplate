package com.app.dto;

import com.app.entity.UserEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자 응답 DTO
 * 
 * 사용자 정보 조회/등록 시 클라이언트에게 반환하는 데이터 구조입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {
    private String userNo;

    private String userId;

    private String userEmail;

    private String userName;

    private String userPhone;

    private String userType;

    private String userAddress;

    public static UserResponse from(UserEntity e) {
        return UserResponse.builder()
                .userNo(e.getUserNo())
                .userId(e.getUserId())
                .userEmail(e.getUserEmail())
                .userName(e.getUserName())
                .userPhone(e.getUserPhone())
                .userType(e.getUserType())
                .userAddress(e.getUserAddress())
                .build();
    }

    public static UserResponse forList(UserEntity e) {
        return UserResponse.builder()
                .userId(e.getUserId())
                .userName(e.getUserName())
                .build();
    }
}
