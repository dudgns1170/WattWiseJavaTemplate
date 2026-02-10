package com.app.controller;

import com.app.common.ApiResponse;
import com.app.dto.UserResponse;
import com.app.dto.UserSignUpRequestDto;
import com.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 사용자 관련 REST API를 제공하는 컨트롤러입니다.
 *
 * <p>
 * - 의존성: 비즈니스 로직을 수행하는 {@link com.app.service.UserService} - 반환 타입: 표현 계층용 DTO인
 * {@link com.app.dto.UserDto}
 * </p>
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "사용자", description = "회원 가입 API")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@Operation(summary = "회원 가입", description = "새 사용자를 등록")
	@PostMapping("/register")
	public ResponseEntity<ApiResponse> register(@Valid @RequestBody UserSignUpRequestDto request) {
		UserResponse response = userService.register(request);
		URI location = URI.create("/api/users/" + response.getUserId());
		ApiResponse body = ApiResponse.created(response);
		return ResponseEntity.created(location).body(body);
	}
 

}
