package com.app.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 제안서 파일 업로드 요청 DTO
 * 
 * 파일 업로드 시 함께 전송하는 메타데이터를 담는 객체입니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProposalFileRequestDto {

	private String userId;

	private String companyName;

	private String siteAddress;

	private String capacity;

	private String createdByName;

	private String updatedByName;
}
