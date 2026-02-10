package com.app.repository;

import com.app.entity.UserEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 사용자 MyBatis 매퍼
 * 
 * user 테이블에 대한 CRUD 작업을 정의합니다.
 */
@MySqlMapper
public interface UserMapper {

    int countById(@Param("userId") String userId);

    int countByEmail(@Param("userEmail") String userEmail);

    int insertUser(UserEntity user);

    List<UserEntity> findAll();

    Optional<UserEntity> findById(@Param("userId") String userId);

    int updateUser(UserEntity user);

    int updateUserDynamic(UserEntity user);

    int deleteById(@Param("userId") String userId);

    List<UserEntity> searchUsers(
        @Param("userId") String userId,
        @Param("userName") String userName,
        @Param("userPh") String userPh
    );

    List<UserEntity> findByIds(@Param("userIds") List<String> userIds);
}
 