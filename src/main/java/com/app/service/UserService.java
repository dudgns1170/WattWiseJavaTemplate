package com.app.service;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.common.BusinessException;
import com.app.common.ErrorCode;
import com.app.dto.UserResponse;
import com.app.dto.UserSignUpRequestDto;
import com.app.entity.UserEntity;
import com.app.mapper.UserEntityMapper;
import com.app.repository.UserMapper;

import lombok.RequiredArgsConstructor;

/**
 * 사용자 도메인의 비즈니스 로직을 담당하는 서비스 레이어입니다.
 *
 * <p>
 * - 외부(Controller)로부터 호출되어 트랜잭션 경계, 검증, 도메인 규칙 등을 수행합니다.
 * - 데이터 접근은 MyBatis 매퍼인 {@link com.app.repository.UserMapper}에 위임합니다.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserEntityMapper userEntityMapper;
    
    /**
     * 회원가입 처리
     */
    @Transactional
    public UserResponse register(UserSignUpRequestDto request) {
        UserEntity entity = userEntityMapper.fromSignUp(request);
        entity.setUserPw(passwordEncoder.encode(request.getUserPw()));
        int insertedRows = userMapper.insertUser(entity);
        if (insertedRows != 1) {
            throw new BusinessException(ErrorCode.USER_CREATE_FAILED);
        }
        return userEntityMapper.toDto(entity);
    }

    /**
     * 간단한 연결 확인용 핑(ping) 메서드입니다.
     * @return 항상 "pong" 문자열을 반환합니다.
     */
    public String ping() { return "pong"; }

    /**
     * 모든 사용자를 조회합니다.
     *
     * <p>
     * 저장소(매퍼) 계층에서 사용자 목록을 읽어와 그대로 반환합니다.
     * </p>
     *
     * @return 전체 사용자 목록
     */
    public List<UserEntity> listUsers() {
        return userMapper.findAll();
    }
}
