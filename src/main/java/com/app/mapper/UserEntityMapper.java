package com.app.mapper;

import com.app.dto.UserResponse;
import com.app.dto.UserSignUpRequestDto;
import com.app.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

import java.util.List;


/**
 * 사용자 Entity ↔ DTO 변환 매퍼
 * 
 * MapStruct를 사용하여 UserEntity와 UserResponse 간 변환을 처리합니다.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserEntityMapper {

    UserResponse toDto(UserEntity user);

    UserEntity toEntity(UserResponse dto);

    List<UserResponse> toDtoList(List<UserEntity> users);

    List<UserEntity> toEntityList(List<UserResponse> dtos);

    UserEntity fromSignUp(UserSignUpRequestDto req);
    
}
