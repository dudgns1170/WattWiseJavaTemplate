package com.app.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 클라이언트 플랫폼 검증 인터셉터
 * 
 * 모든 API 요청에 X-Client-Platform 헤더가 포함되어 있는지 검증합니다.
 * 허용되는 값: "web", "app"
 */
@Slf4j
@Component
public class ClientPlatformInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME = "X-Client-Platform";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String platform = request.getHeader(HEADER_NAME);

        if (platform == null || platform.isBlank()) {
            throw new BusinessException(ErrorCode.MISSING_CLIENT_PLATFORM_HEADER);
        }

        String normalized = platform.toLowerCase();
        if (!"web".equals(normalized) && !"app".equals(normalized)) {
            throw new BusinessException(ErrorCode.INVALID_CLIENT_PLATFORM);
        }

        log.debug("[ClientPlatform] platform={}, method={}, uri={}", 
                  normalized, request.getMethod(), request.getRequestURI());

        return true;
    }
}