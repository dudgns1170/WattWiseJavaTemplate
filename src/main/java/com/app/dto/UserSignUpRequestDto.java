package com.app.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * 회원가입 요청 DTO
 * 
 * 사용자 등록 시 클라이언트에서 전송하는 데이터를 담는 객체입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserSignUpRequestDto {
    @NotBlank
    @Size(min = 4, max = 50, message = "userId는 4~50자여야 합니다.")
    private String userId;

    @NotBlank
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @Size(max = 100, message = "이메일은 최대 100자입니다.")
    private String userEmail;

    @NotBlank
    @Size(min = 8, max = 72, message = "비밀번호는 8~72자여야 합니다.")
    private String userPw;

    @NotBlank
    @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다.")
    private String userName;

    @NotBlank
    @Size(max = 20, message = "전화번호는 최대 20자입니다.")
    @Pattern(regexp = "^[0-9\\-+]{9,20}$", message = "전화번호 형식이 올바르지 않습니다.")
    @JsonAlias("userPh")
    private String userPhone;
    
    @NotBlank(message = "userType은 필수입니다.")
    @Pattern(regexp = "^[AUCS]$", message = "userType은 A, U, C, S 중 하나여야 합니다.")
    private String userType;

    @Size(max = 200, message = "주소는 최대 200자입니다.")
    private String userAddress;
}