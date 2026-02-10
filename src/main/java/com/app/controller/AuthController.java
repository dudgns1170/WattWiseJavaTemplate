package com.app.controller;

import com.app.common.ApiResponse;
import com.app.common.BusinessException;
import com.app.common.ErrorCode;
import com.app.config.AppProps;
import com.app.dto.LoginRequestDto;
import com.app.dto.RefreshRequestDto;
import com.app.dto.TokenResponseDto;
import com.app.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 인증(Authentication) REST API 컨트롤러
 * 
 * 로그인, 토큰 갱신, 로그아웃 API를 제공합니다.
 * SecurityConfig에서 이 엔드포인트들은 인증 없이 접근 가능하도록 설정되어 있습니다.
 * 
 * API Base Path: /api/auth
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "인증", description = "로그인/토큰 API")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;  // 인증 비즈니스 로직 서비스
    private final AppProps appProps;

    /**
     * 로그인 API
     * 
     * 사용자 인증 후 Access Token과 Refresh Token을 발급합니다.
     * - web: Refresh Token은 HttpOnly 쿠키로만 저장/전달, 응답 바디에는 Access Token만 반환
     * - app: 응답 바디에 Access/Refresh Token을 모두 반환
     * 
     * [요청 예시]
     * POST /api/auth/login
     * Content-Type: application/json
     * 
     * {
     *   "userId": "user123",
     *   "password": "password123"
     * }
     * 
     * [응답 예시]
     * 200 OK
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * 
     * @param request 로그인 요청 (userId, password)
     * @return 발급된 토큰 쌍 (Access Token + Refresh Token)
     */
    @Operation(summary = "로그인", description = "사용자 인증 후 Access/Refresh 토큰 발급")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse> login(
            @RequestBody LoginRequestDto request,
            @RequestHeader("X-Client-Platform") String clientPlatform,
            HttpServletResponse response) {
        // AuthService에서 사용자 검증 및 토큰 발급 처리
        TokenResponseDto tokens = authService.login(request);

        String normalizedPlatform = normalizePlatform(clientPlatform);

        if ("web".equals(normalizedPlatform)) {
            // web: Refresh Token은 HttpOnly 쿠키로만 전달/회전. (JS에서 접근 불가)
            // 쿠키 만료는 app.jwt.refresh-ttl-days 설정 값과 동일하게 유지
            Cookie refreshCookie = new Cookie("refreshToken", tokens.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(refreshCookieMaxAgeSeconds());
            refreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(refreshCookie);

            TokenResponseDto bodyTokens = new TokenResponseDto(tokens.getAccessToken(), null);
            return ResponseEntity.ok(ApiResponse.ok(bodyTokens));
        }

        return ResponseEntity.ok(ApiResponse.ok(tokens));
    }

    /**
     * 토큰 갱신 API (Refresh)
     * 
     * Access Token이 만료되었을 때, Refresh Token을 사용하여 새로운 토큰 쌍을 발급받습니다.
     * Token Rotation 전략을 사용하여 Refresh Token도 함께 갱신됩니다.
     * - web: Refresh Token을 쿠키에서 우선 읽고, 회전된 Refresh Token을 다시 쿠키로 세팅
     * - app: Refresh Token을 바디에서 우선 읽고, 응답 바디에 새 Refresh Token을 함께 반환
     * 
     * [요청 예시]
     * POST /api/auth/refresh
     * Content-Type: application/json
     * 
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     * 
     * [응답 예시]
     * 200 OK
     * {
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",  (새로 발급)
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."   (새로 발급)
     * }
     * 
     * @param request Refresh Token을 담은 요청
     * @return 새로 발급된 토큰 쌍
     */
    @Operation(summary = "토큰 갱신", description = "Refresh Token으로 새로운 Access/Refresh 토큰 발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse> refresh(
            @RequestHeader("X-Client-Platform") String clientPlatform,
            @RequestBody(required = false) RefreshRequestDto request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {

        String normalizedPlatform = normalizePlatform(clientPlatform);

        String refreshTokenFromCookie = resolveCookieValue(httpServletRequest, "refreshToken");
        String refreshTokenFromBody = request != null ? request.getRefreshToken() : null;

        String refreshToken;
        if ("web".equals(normalizedPlatform)) {
            // 신규: web은 쿠키 기반 동작을 우선한다. (refresh token을 JS에서 다루지 않게)
            refreshToken = hasText(refreshTokenFromCookie) ? refreshTokenFromCookie : refreshTokenFromBody;
        } else {
            // 신규: app은 바디 기반 동작을 우선한다. (쿠키/브라우저 정책에 의존하지 않게)
            refreshToken = hasText(refreshTokenFromBody) ? refreshTokenFromBody : refreshTokenFromCookie;
        }

        if (!hasText(refreshToken)) {
            throw new BusinessException(ErrorCode.TOKEN_MISSING);
        }

        TokenResponseDto refreshedTokens = authService.refresh(new RefreshRequestDto(refreshToken));

        if ("web".equals(normalizedPlatform)) {
            // web: rotation된 refresh token을 쿠키로 다시 세팅 (다음 refresh에서 재사용/탈취 탐지 실패 방지)
            Cookie refreshCookie = new Cookie("refreshToken", refreshedTokens.getRefreshToken());
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(refreshCookieMaxAgeSeconds());
            refreshCookie.setAttribute("SameSite", "Strict");
            httpServletResponse.addCookie(refreshCookie);

            TokenResponseDto bodyTokens = new TokenResponseDto(refreshedTokens.getAccessToken(), null);
            return ResponseEntity.ok(ApiResponse.ok(bodyTokens));
        }

        return ResponseEntity.ok(ApiResponse.ok(refreshedTokens));
    }

    /**
     * 로그아웃 API
     * 
     * 현재 세션/디바이스 단위로 Refresh Token을 무효화합니다.
     * (Authorization 헤더의 Access Token에 포함된 fid(familyId)를 기준으로 Redis 키를 특정)
     * 
     * [요청 예시]
     * POST /api/auth/logout
     * Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
     * 
     * [응답 예시]
     * 200 OK
     * 
     * 참고: Access Token은 무상태(Stateless)이므로 클라이언트에서 폐기해야 합니다.
     *      서버는 만료 시간까지 해당 토큰의 유효성을 검증할 수 있습니다.
     * 
     * @param authHeader Authorization 헤더 (Bearer {token})
     * @return 200 OK (공통 응답 포맷)
     */
    @Operation(summary = "로그아웃", description = "사용자의 Refresh Token을 모두 무효화")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @RequestHeader("X-Client-Platform") String clientPlatform,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            HttpServletResponse response) {
        if (!hasText(authHeader)) {
            throw new BusinessException(ErrorCode.TOKEN_MISSING);
        }
        // AuthService에서 Redis의 Refresh Token 삭제
        authService.logout(authHeader);

        String normalizedPlatform = normalizePlatform(clientPlatform);
        if ("web".equals(normalizedPlatform)) {
            Cookie refreshCookie = new Cookie("refreshToken", "");
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(true);
            refreshCookie.setPath("/");
            refreshCookie.setMaxAge(0);
            refreshCookie.setAttribute("SameSite", "Strict");
            response.addCookie(refreshCookie);
        }
        
        // 200 OK + 공통 응답 포맷으로 반환
        return ResponseEntity.ok(ApiResponse.success(HttpStatus.OK, "로그아웃 완료", null));
    }

    private String resolveCookieValue(HttpServletRequest request, String cookieName) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private int refreshCookieMaxAgeSeconds() {
        // 신규: Cookie Max-Age를 JWT/Redis TTL과 동일한 설정(app.jwt.refresh-ttl-days)에서 파생
        long seconds = java.time.Duration.ofDays(appProps.getJwt().getRefreshTtlDays()).getSeconds();
        return Math.toIntExact(seconds);
    }

    private String normalizePlatform(String clientPlatform) {
        if (clientPlatform == null) {
            return "";
        }
        return clientPlatform.trim().toLowerCase();
    }
}
