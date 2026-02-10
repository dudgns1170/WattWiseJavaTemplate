package com.app.service;

import com.app.common.BusinessException;
import com.app.common.ErrorCode;
import com.app.config.AppProps;
import com.app.dto.LoginRequestDto;
import com.app.dto.RefreshRequestDto;
import com.app.dto.TokenResponseDto;
import com.app.entity.UserEntity;
import com.app.repository.UserMapper;
import com.app.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 인증(Authentication) 비즈니스 로직 서비스
 * 
 * JWT 기반 로그인, 토큰 갱신, 로그아웃 기능을 제공합니다.
 * Refresh Token은 Redis에 저장하여 서버 측에서도 관리합니다.
 */
@Service
@RequiredArgsConstructor
public class AuthService { 

    private final JwtService jwtService;                          // JWT 발급/파싱 서비스
    private final RedisTemplate<String, String> redisTemplate;    // Redis 작업 템플릿
    private final UserMapper userMapper;                          // 사용자 DB 조회 매퍼
    private final PasswordEncoder passwordEncoder;                // 비밀번호 암호화 검증
    private final AppProps appProps;

    /**
     * 로그인 
     * 
     * 사용자 인증 후 Access Token과 Refresh Token을 발급합니다.
     * Refresh Token은 Redis에 저장하여 서버 측에서도 유효성을 관리합니다.
     * 
     * @param req 로그인 요청 (userId, password)
     * @return 발급된 토큰 쌍 (Access Token + Refresh Token)
     * @throws RuntimeException 사용자가 없거나 비밀번호가 틀린 경우
     */
    public TokenResponseDto login(LoginRequestDto req) {
        // 1. 사용자 존재 확인
        UserEntity user = userMapper.findById(req.getUserId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 비밀번호 검증
        // DB에 저장된 BCrypt 해시 값과 입력된 평문 비밀번호를 비교
        if (!passwordEncoder.matches(req.getPassword(), user.getUserPw())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        // 3. Refresh Token 발급 (장기 토큰, app.jwt.refresh-ttl-days 설정 기반)
        // Family ID: 같은 로그인 세션을 그룹화하는 식별자 (토큰 로테이션 추적용)
        String familyId = UUID.randomUUID().toString();
        // JTI (JWT ID): 개별 토큰의 고유 식별자 (중복 사용 방지)
        String jti = UUID.randomUUID().toString();
        String refreshToken = jwtService.issueRefresh(user.getUserId(), familyId, jti);

        // 4. Access Token 발급 (단기 토큰, app.jwt.access-ttl-minutes 설정 기반)
        // subject: 사용자 ID, claims: 현재 세션(디바이스) 식별을 위한 fid 포함
        String accessToken = jwtService.issueAccess(user.getUserId(), Map.of("fid", familyId));

        // 5. Refresh Token을 Redis에 저장
        // Key 형식: "RT:{userId}:{familyId}"
        // Value: JTI (토큰 고유 ID)
        // TTL: app.jwt.refresh-ttl-days 설정 값과 동일하게 유지 (JWT exp / Redis TTL 불일치 방지)
        String redisKey = "RT:" + user.getUserId() + ":" + familyId;
        redisTemplate.opsForValue().set(redisKey, jti, Duration.ofDays(appProps.getJwt().getRefreshTtlDays()));

        // 6. 클라이언트에게 토큰 쌍 반환
        return new TokenResponseDto(accessToken, refreshToken);
    }
   

    /**
     * Refresh Token으로 새 토큰 발급 (Token Rotation)
     * 
     * 만료된 Access Token을 Refresh Token을 사용하여 갱신합니다.
     * Refresh Token도 함께 새로 발급하여 보안을 강화합니다 (Rotation 전략).
     * 
     * @param req Refresh Token을 담은 요청
     * @return 새로 발급된 토큰 쌍
     * @throws RuntimeException Refresh Token이 유효하지 않거나 Redis 검증 실패 시
     */
    public TokenResponseDto refresh(RefreshRequestDto req) {
        // 1. Refresh Token 파싱 및 검증
        Claims claims = jwtService.parse(req.getRefreshToken());
        
        String userId = claims.getSubject();              // 사용자 ID
        String familyId = (String) claims.get("fid");     // Family ID
        String jti = (String) claims.get("jti");          // Token ID

        // 2. Redis에서 저장된 JTI 조회
        // Redis에 없거나 값이 다르면 탈취/재사용된 토큰으로 간주
        String redisKey = "RT:" + userId + ":" + familyId;
        String storedJti = redisTemplate.opsForValue().get(redisKey);

        if (storedJti == null || !jti.equals(storedJti)) {
            // 보안: 탈취 의심 시 해당 Family의 모든 토큰 무효화 권장
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 3. 새로운 Access Token 발급
        // access token에도 fid를 포함시켜 "현재 세션/디바이스 단위" 로그아웃이 가능하도록 함
        String newAccessToken = jwtService.issueAccess(userId, Map.of("fid", familyId));

        // 4. 새로운 Refresh Token 발급 (Rotation)
        // Family ID는 유지하되 JTI는 새로 생성하여 재사용 방지
        String newJti = UUID.randomUUID().toString();
        String newRefreshToken = jwtService.issueRefresh(userId, familyId, newJti);

        // 5. Redis 업데이트: 새 JTI로 교체
        redisTemplate.opsForValue().set(redisKey, newJti, Duration.ofDays(appProps.getJwt().getRefreshTtlDays()));

        // 6. 새 토큰 쌍 반환
        return new TokenResponseDto(newAccessToken, newRefreshToken);
    }

    /**
     * 로그아웃 처리
     * 
     * 현재 세션/디바이스 단위로 Refresh Token을 Redis에서 삭제하여 무효화합니다.
     * (Access Token의 fid(familyId)를 사용해 대상 Redis Key를 특정)
     * 클라이언트는 보유한 Access Token도 함께 폐기해야 합니다.
     * 
     * @param authHeader Authorization 헤더 값 (Bearer {token})
     */
    
    public void logout(String authHeader) {
        // 1. Authorization 헤더에서 토큰 추출
        // 형식: "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.TOKEN_MISSING);
        }
        
        String token = authHeader.substring(7);  // "Bearer " 제거

        // 2. 토큰에서 사용자 ID 추출 (만료 토큰도 허용)
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (ExpiredJwtException eje) {
            claims = eje.getClaims();
        }
        if (claims == null) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }
        String userId = claims.getSubject();

        // 3. "현재 세션/디바이스"(familyId) 단위로 Refresh Token 무효화
        // 신규: access token에 포함된 fid로 Redis Key를 특정하여 KEYS() 사용을 피함
        String familyId = (String) claims.get("fid");
        if (familyId == null || familyId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String redisKey = "RT:" + userId + ":" + familyId;
        redisTemplate.delete(redisKey);

        // 참고: Access Token은 무상태(Stateless)이므로 서버에서 강제 만료 불가
        // 클라이언트가 토큰을 폐기하고, 만료 시간까지 대기해야 함
    }
}
