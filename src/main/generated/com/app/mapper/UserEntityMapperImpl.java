package com.app.mapper;

import com.app.dto.UserResponse;
import com.app.dto.UserSignUpRequestDto;
import com.app.entity.UserEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-16T15:31:34+0900",
    comments = "version: 1.6.2, compiler: javac, environment: Java 17.0.16 (Oracle Corporation)"
)
@Component
public class UserEntityMapperImpl implements UserEntityMapper {

    @Override
    public UserResponse toDto(UserEntity user) {
        if ( user == null ) {
            return null;
        }

        UserResponse.UserResponseBuilder userResponse = UserResponse.builder();

        userResponse.userNo( user.getUserNo() );
        userResponse.userId( user.getUserId() );
        userResponse.userEmail( user.getUserEmail() );
        userResponse.userName( user.getUserName() );
        userResponse.userPhone( user.getUserPhone() );
        userResponse.userType( user.getUserType() );
        userResponse.userAddress( user.getUserAddress() );

        return userResponse.build();
    }

    @Override
    public UserEntity toEntity(UserResponse dto) {
        if ( dto == null ) {
            return null;
        }

        UserEntity.UserEntityBuilder userEntity = UserEntity.builder();

        userEntity.userNo( dto.getUserNo() );
        userEntity.userId( dto.getUserId() );
        userEntity.userEmail( dto.getUserEmail() );
        userEntity.userName( dto.getUserName() );
        userEntity.userPhone( dto.getUserPhone() );
        userEntity.userType( dto.getUserType() );
        userEntity.userAddress( dto.getUserAddress() );

        return userEntity.build();
    }

    @Override
    public List<UserResponse> toDtoList(List<UserEntity> users) {
        if ( users == null ) {
            return null;
        }

        List<UserResponse> list = new ArrayList<UserResponse>( users.size() );
        for ( UserEntity userEntity : users ) {
            list.add( toDto( userEntity ) );
        }

        return list;
    }

    @Override
    public List<UserEntity> toEntityList(List<UserResponse> dtos) {
        if ( dtos == null ) {
            return null;
        }

        List<UserEntity> list = new ArrayList<UserEntity>( dtos.size() );
        for ( UserResponse userResponse : dtos ) {
            list.add( toEntity( userResponse ) );
        }

        return list;
    }

    @Override
    public UserEntity fromSignUp(UserSignUpRequestDto req) {
        if ( req == null ) {
            return null;
        }

        UserEntity.UserEntityBuilder userEntity = UserEntity.builder();

        userEntity.userId( req.getUserId() );
        userEntity.userEmail( req.getUserEmail() );
        userEntity.userPw( req.getUserPw() );
        userEntity.userName( req.getUserName() );
        userEntity.userPhone( req.getUserPhone() );
        userEntity.userType( req.getUserType() );
        userEntity.userAddress( req.getUserAddress() );

        return userEntity.build();
    }
}
