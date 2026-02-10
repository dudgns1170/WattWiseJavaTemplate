package com.app.mapper;

import com.app.dto.ProposalFileRequestDto;
import com.app.dto.ProposalFileResponseDto;
import com.app.entity.ProposalFileEntity;
import com.app.service.S3StorageService;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 제안서 파일 Entity ↔ DTO 변환 매퍼
 * 
 * MapStruct를 사용하여 ProposalFileEntity와 ProposalFileResponseDto 간 변환을 처리합니다.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ProposalFileEntityMapper {

	@Mapping(target = "pr_no", source = "proposalNo")
	@Mapping(target = "pr_user_id", source = "request.userId")
	@Mapping(target = "pr_co_name", source = "request.companyName")
	@Mapping(target = "pr_site_address", source = "request.siteAddress")
	@Mapping(target = "pr_capacity", source = "request.capacity")
	@Mapping(target = "pr_file_name", source = "originalFileName")
	@Mapping(target = "pr_file_object_key", source = "uploadResult.key")
	@Mapping(target = "pr_file_storage_url", source = "uploadResult.url")
	@Mapping(target = "pr_file_created_by_name", source = "request.createdByName")
	@Mapping(target = "pr_file_updated_by_name", source = "request.updatedByName")
	@Mapping(target = "created_at", source = "createdAt")
	ProposalFileEntity toEntity(String proposalNo,
	                            ProposalFileRequestDto request,
	                            String originalFileName,
	                            S3StorageService.UploadResult uploadResult,
	                            LocalDateTime createdAt);

	@Mapping(target = "proposalNo", source = "pr_no")
	@Mapping(target = "userId", source = "pr_user_id")
	@Mapping(target = "companyName", source = "pr_co_name")
	@Mapping(target = "siteAddress", source = "pr_site_address")
	@Mapping(target = "capacity", source = "pr_capacity")
	@Mapping(target = "fileName", source = "pr_file_name")
	@Mapping(target = "fileUrl", source = "pr_file_storage_url")
	@Mapping(target = "createdName", source = "pr_file_created_by_name")
	@Mapping(target = "updateName", source = "pr_file_updated_by_name")
	@Mapping(target = "createdDt", source = "created_at")
	@Mapping(target = "updateDt", source = "updated_at")
	ProposalFileResponseDto toDto(ProposalFileEntity entity);

    List<ProposalFileResponseDto> fileDtoList(List<ProposalFileEntity> entities);

    
}