package com.app.mapper;

import com.app.dto.ProposalFileRequestDto;
import com.app.dto.ProposalFileResponseDto;
import com.app.entity.ProposalFileEntity;
import com.app.service.S3StorageService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-01-16T15:31:33+0900",
    comments = "version: 1.6.2, compiler: javac, environment: Java 17.0.16 (Oracle Corporation)"
)
@Component
public class ProposalFileEntityMapperImpl implements ProposalFileEntityMapper {

    @Override
    public ProposalFileEntity toEntity(String proposalNo, ProposalFileRequestDto request, String originalFileName, S3StorageService.UploadResult uploadResult, LocalDateTime createdAt) {
        if ( proposalNo == null && request == null && originalFileName == null && uploadResult == null && createdAt == null ) {
            return null;
        }

        ProposalFileEntity.ProposalFileEntityBuilder proposalFileEntity = ProposalFileEntity.builder();

        if ( request != null ) {
            proposalFileEntity.pr_user_id( request.getUserId() );
            proposalFileEntity.pr_co_name( request.getCompanyName() );
            proposalFileEntity.pr_site_address( request.getSiteAddress() );
            proposalFileEntity.pr_capacity( request.getCapacity() );
            proposalFileEntity.pr_file_created_by_name( request.getCreatedByName() );
            proposalFileEntity.pr_file_updated_by_name( request.getUpdatedByName() );
        }
        if ( uploadResult != null ) {
            proposalFileEntity.pr_file_object_key( uploadResult.key() );
            proposalFileEntity.pr_file_storage_url( uploadResult.url() );
        }
        proposalFileEntity.pr_no( proposalNo );
        proposalFileEntity.pr_file_name( originalFileName );
        proposalFileEntity.created_at( createdAt );

        return proposalFileEntity.build();
    }

    @Override
    public ProposalFileResponseDto toDto(ProposalFileEntity entity) {
        if ( entity == null ) {
            return null;
        }

        ProposalFileResponseDto.ProposalFileResponseDtoBuilder proposalFileResponseDto = ProposalFileResponseDto.builder();

        proposalFileResponseDto.proposalNo( entity.getPr_no() );
        proposalFileResponseDto.userId( entity.getPr_user_id() );
        proposalFileResponseDto.companyName( entity.getPr_co_name() );
        proposalFileResponseDto.siteAddress( entity.getPr_site_address() );
        proposalFileResponseDto.capacity( entity.getPr_capacity() );
        proposalFileResponseDto.fileName( entity.getPr_file_name() );
        proposalFileResponseDto.fileUrl( entity.getPr_file_storage_url() );
        proposalFileResponseDto.createdName( entity.getPr_file_created_by_name() );
        proposalFileResponseDto.updateName( entity.getPr_file_updated_by_name() );
        proposalFileResponseDto.createdDt( entity.getCreated_at() );
        proposalFileResponseDto.updateDt( entity.getUpdated_at() );

        return proposalFileResponseDto.build();
    }

    @Override
    public List<ProposalFileResponseDto> fileDtoList(List<ProposalFileEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<ProposalFileResponseDto> list = new ArrayList<ProposalFileResponseDto>( entities.size() );
        for ( ProposalFileEntity proposalFileEntity : entities ) {
            list.add( toDto( proposalFileEntity ) );
        }

        return list;
    }
}
