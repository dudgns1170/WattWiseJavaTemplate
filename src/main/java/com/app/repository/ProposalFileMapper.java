package com.app.repository;

import com.app.entity.ProposalFileEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Optional;

/**
 * 제안서 파일 MyBatis 매퍼
 * 
 * proposal_file 테이블에 대한 CRUD 작업을 정의합니다.
 */
@MySqlMapper
public interface ProposalFileMapper {
	
	int insertFile(ProposalFileEntity file);
	List<ProposalFileEntity> findByProposalNo(String proposalNo);
	
	List<ProposalFileEntity> fileDtoList();
	Optional<ProposalFileEntity> findById(Long fileId);
	Optional<ProposalFileEntity> findByIdAndProposalNo(@Param("fileId") Long fileId,
            @Param("proposalNo") String proposalNo);
	int updateFile(ProposalFileEntity file);
	int delete(Long fileId);
	
	

}
