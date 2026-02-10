package com.app.dto;

import com.app.entity.ProposalFileEntity;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 제안서 파일 응답 DTO
 * 
 * 제안서 파일 업로드/조회 시 클라이언트에게 반환하는 데이터 구조입니다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProposalFileResponseDto {
	private String proposalNo;
	private String userId;
	private String companyName;
	private String siteAddress;
	private String capacity;
	private String fileName;
	private String fileUrl;
	private String createdName;
	private String updateName;
	private LocalDateTime  createdDt;
	private LocalDateTime  updateDt;

	
	public static ProposalFileResponseDto from(ProposalFileEntity entity) {
        return ProposalFileResponseDto.builder()
                .proposalNo(entity.getPr_no())
                .userId(entity.getPr_user_id())
                .companyName(entity.getPr_co_name())
                .siteAddress(entity.getPr_site_address())
                .capacity(entity.getPr_capacity())
                .fileName(entity.getPr_file_name())
                .fileUrl(entity.getPr_file_storage_url())
                .createdName(entity.getPr_file_created_by_name())
                .updateName(entity.getPr_file_updated_by_name())
                .createdDt(entity.getCreated_at())
                .updateDt(entity.getUpdated_at())
                .build();
	}
	
}
