package com.app.security;

import com.app.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * JWT 인증 필터
 * 
 * 모든 요청에 대해 Access Token을 검증하고 인증 정보를 SecurityContext에 설정합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest req,
            @NonNull HttpServletResponse res,
            @NonNull FilterChain chain) throws ServletException, IOException {

        // 1) Preflight는 통과
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            chain.doFilter(req, res);
            return;
        }

        String token = resolveBearer(req);

        if (token != null) {
            try {
                // 2) 만료/서명 검증 포함. parse는 Claims를 반환하도록 JwtService에서 일원화
                Claims c = jwtService.parse(token);

                // 3) Access 토큰만 인증 처리
                if (Objects.equals("at", c.get("typ"))) {
                    String userId = c.getSubject();

                    // (선택) 권한 확장 포인트: roles 클레임을 SimpleGrantedAuthority로 매핑
                    var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

                    var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }

            } catch (ExpiredJwtException eje) {
                SecurityContextHolder.clearContext();
                log.info("[JwtAuthFilter] Access token expired for sub={}", safeSubject(eje));

                writeUnauthorized(
                        res,
                        401,
                        "TOKEN_EXPIRED",
                        "Access token expired",
                        "Bearer error=\"invalid_token\", error_description=\"token expired\""
                );
                return; // 체인 중단

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                log.info("[JwtAuthFilter] Invalid access token: {}", e.getMessage());

                writeUnauthorized(
                        res,
                        401,
                        "INVALID_TOKEN",
                        "Invalid access token",
                        "Bearer error=\"invalid_token\""
                );
                return; // 체인 중단
            }
        }

        // 토큰이 없거나, 유효 → 다음 필터
        chain.doFilter(req, res);
    }

    // /api/auth/* 경로는 필터 스킵 (로그인/리프레시/로그아웃 등)
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null) return false;
        // 필요시 정교하게 조정 (정규식/화이트리스트)
        return path.startsWith("/api/auth/");
    }

    private String resolveBearer(HttpServletRequest req) {
        String h = req.getHeader(AUTH_HEADER);
        if (h != null && h.startsWith(BEARER_PREFIX)) {
            return h.substring(BEARER_PREFIX.length()).trim();
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse res,
                                   int status,
                                   String code,
                                   String message,
                                   String wwwAuthenticate) throws IOException {
        if (res.isCommitted()) {
            return;
        }
        ApiResponse body = ApiResponse.error(HttpStatus.valueOf(status), message);
        res.setStatus(status);
        if (wwwAuthenticate != null && !wwwAuthenticate.isBlank()) {
            res.setHeader("WWW-Authenticate", wwwAuthenticate);
        }
        res.setHeader("Cache-Control", "no-store"); // 권장
        res.setContentType("application/json;charset=UTF-8");
        objectMapper.writeValue(res.getWriter(), body);
        res.getWriter().flush();
    }

    private String safeSubject(ExpiredJwtException eje) {
        try {
            return eje.getClaims() != null ? eje.getClaims().getSubject() : "unknown";
        } catch (Exception ignore) {
            return "unknown";
        }
    }
}
