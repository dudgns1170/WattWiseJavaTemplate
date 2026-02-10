package com.app.controller;

import com.app.common.ApiResponse;
import com.app.service.MailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mail")
@RequiredArgsConstructor
@Tag(name = "이메일", description = "이메일 인증 코드 전송 및 검증 API")
public class MailController {
    private final MailService mailService;

    /**
     * 인증 메일 전송 API
     * 
     * @param email 인증 코드를 받을 이메일 주소
     * @return 전송 완료 메시지
     */
    @Operation(summary = "인증 메일 전송", description = "입력한 이메일 주소로 인증 코드를 발송")
    @PostMapping("/send")
    public ResponseEntity<ApiResponse> sendMail(@RequestParam("email") @Valid String email) {
        mailService.sendCodeToEmail(email);
        ApiResponse body = ApiResponse.success(HttpStatus.OK.value(), "인증 메일 전송 완료", null);
        return ResponseEntity.ok(body);
    }
    /**
     * 인증 코드 검증 API
     * 
     * @param payload email과 code를 담은 요청 본문
     * @return 인증 성공 메시지
     */
    @Operation(summary = "인증 코드 검증", description = "이메일과 인증 코드를 검증")
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse> verifyCode(@RequestBody Map<String, String> payload){
    	String email = payload.get("email");
    	String code = payload.get("code");
    	  mailService.verifyCode(email, code);
          ApiResponse body = ApiResponse.success(HttpStatus.OK.value(), "이메일 인증 성공", null);
          return ResponseEntity.ok(body);
    }
    
}
