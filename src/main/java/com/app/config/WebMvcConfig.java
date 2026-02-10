package com.app.config;

import com.app.common.ClientPlatformInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Spring MVC 설정 클래스
 * 
 * 인터셉터 등록 등 MVC 관련 설정을 정의합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final ClientPlatformInterceptor clientPlatformInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientPlatformInterceptor)
                .addPathPatterns("/api/**");
    }
}
