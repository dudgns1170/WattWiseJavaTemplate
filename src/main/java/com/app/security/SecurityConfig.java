package com.app.security;

import com.app.common.ApiResponse;
import com.app.config.AppProps;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 설정 클래스
 * 
 * JWT 기반 인증, CORS, 권한 설정을 정의합니다.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AppProps appProps;
    private final ObjectMapper objectMapper;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 세션 사용 안 함 (JWT 무상태)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // CORS: 화이트리스트 기반
            .cors(c -> c.configurationSource(corsConfigurationSource()))

            // CSRF: 기본은 비활성화. (web에서 RT를 쿠키로 쓰면 /api/auth/refresh 에 Double Submit 적용 권장)
            .csrf(csrf -> csrf.disable())

            // 권한
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/health",
                    "/h2-console/**",
                    "/public/**",
                    "/api/file/**",
                    "/api/proposals/",
                    "/api/rtu/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).permitAll()
                .requestMatchers(HttpMethod.POST,
                    "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                    "/api/users/register",
                    "/api/mail/send", "/api/mail/verify"
                ).permitAll()
                .anyRequest().authenticated()
            )

            // 인증/인가 실패 응답을 ApiResponse 포맷으로 통일
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req, res, ex) -> {
                    if (res.isCommitted()) {
                        return;
                    }
                    ApiResponse body = ApiResponse.error(HttpStatus.UNAUTHORIZED, "authentication_required");
                    res.setStatus(HttpStatus.UNAUTHORIZED.value());
                    res.setHeader("Cache-Control", "no-store");
                    res.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(res.getWriter(), body);
                    res.getWriter().flush();
                })
                .accessDeniedHandler((req, res, ex) -> {
                    if (res.isCommitted()) {
                        return;
                    }
                    ApiResponse body = ApiResponse.error(HttpStatus.FORBIDDEN, "access_denied");
                    res.setStatus(HttpStatus.FORBIDDEN.value());
                    res.setHeader("Cache-Control", "no-store");
                    res.setContentType("application/json;charset=UTF-8");
                    objectMapper.writeValue(res.getWriter(), body);
                    res.getWriter().flush();
                })
            )

            // JWT 필터 삽입
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // yml: app.cors.allowed-origins: http://localhost:3000,http://localhost:5173
        List<String> origins = Arrays.stream(appProps.getCors().getAllowedOrigins().split(","))
                                     .map(String::trim).toList();
        cfg.setAllowedOrigins(origins);
        cfg.setAllowedMethods(Arrays.asList("GET","POST","PUT","DELETE","OPTIONS"));
        cfg.setAllowedHeaders(Arrays.asList("Authorization","Content-Type","X-XSRF-TOKEN","X-Client-Platform"));
        cfg.setAllowCredentials(true); // RT 쿠키 쓸 때 필요
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", cfg);
        return src;
    }

    // 비밀번호 인코더 (로그인 검증에 사용)
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
