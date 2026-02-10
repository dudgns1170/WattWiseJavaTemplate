package com.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 클래스
 * 
 * Redis를 사용하여 Refresh Token을 저장/관리하기 위한 설정입니다.
 * RedisTemplate을 Spring Bean으로 등록하여 애플리케이션 전역에서 사용할 수 있도록 합니다.
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate Bean 등록
     * 
     * Redis에 문자열 Key-Value를 저장/조회하기 위한 템플릿을 생성합니다.
     * 
     * @param connectionFactory Spring Boot가 자동으로 주입하는 Redis 연결 팩토리
     *                          (application.yml의 spring.data.redis 설정 기반)
     * @return Redis 작업을 위한 RedisTemplate 인스턴스
     */
    @Bean
    @Primary
    RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        
        // Redis 연결 설정 (application.yml에서 자동 주입)
        template.setConnectionFactory(connectionFactory);
        
        // Key 직렬화: 문자열을 그대로 Redis에 저장
        // 예: "RT:user123:family-uuid" → 그대로 저장
        template.setKeySerializer(new StringRedisSerializer());
        
        // Value 직렬화: JTI(Token ID)를 문자열로 저장
        // 예: "550e8400-e29b-41d4-a716-446655440000" → 그대로 저장
        template.setValueSerializer(new StringRedisSerializer());
        
        // Hash Key/Value도 문자열로 직렬화 (필요 시 Hash 구조 사용)
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        return template;
    }
}
