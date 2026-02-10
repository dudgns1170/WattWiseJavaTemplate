package com.app.service;

import com.app.dto.ProposalFileRequestDto;
import com.app.dto.ProposalFileResponseDto;
import com.app.entity.ProposalFileEntity;
import com.app.mapper.ProposalFileEntityMapper;
import com.app.repository.ProposalFileMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 제안서 파일 관리 비즈니스 로직 서비스
 * 
 * <p>이 서비스는 제안서 파일의 업로드, 조회, 삭제 등의 핵심 비즈니스 로직을 처리합니다.
 * S3 스토리지 작업과 DB 영속성 작업을 조율하며, 인증된 사용자 정보를 자동으로 채워넣는 등의
 * 부가 기능도 제공합니다.</p>
 * 
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>파일 업로드: S3에 파일 저장 + DB에 메타데이터 저장</li>
 *   <li>파일 목록 조회: 제안서 번호로 파일 목록 반환</li>
 *   <li>파일 삭제: S3와 DB에서 파일 제거</li>
 *   <li>사용자 ID 자동 채움: 요청에 userId가 없으면 현재 로그인 사용자로 자동 설정</li>
 *   <li>업로드 경로 정규화: document/ 중복 방지</li>
 * </ul>
 * 
 * @see com.app.controller.ProposalFileController
 * @see com.app.service.S3StorageService
 * @see com.app.repository.ProposalFileMapper
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ProposalFileService {

	// MyBatis 매퍼: proposal_file 테이블 CRUD 작업
	private final ProposalFileMapper proposalFileMapper;
	
	// MapStruct 매퍼: DTO ↔ Entity 변환
	private final ProposalFileEntityMapper proposalFileEntityMapper;
	
	// S3 스토리지 서비스: 파일 업로드/삭제/URL 생성
	private final S3StorageService s3StorageService;

	/**
	 * 제안서 파일 업로드 처리
	 * 
	 * <p>이 메서드는 다음 작업을 순차적으로 수행합니다:</p>
	 * <ol>
	 *   <li>요청 DTO 검증 및 userId 자동 채움 (비어있을 경우 현재 로그인 사용자 ID 사용)</li>
	 *   <li>S3 업로드 경로 정규화 (현 단계에서는 document/ 루트에 고정)</li>
	 *   <li>S3에 파일 업로드 (랜덤 파일명 생성)</li>
	 *   <li>엔티티 생성 (MapStruct를 통해 DTO → Entity 변환)</li>
	 *   <li>DB에 파일 메타데이터 저장 (proposal_file 테이블 INSERT)</li>
	 *   <li>저장된 엔티티를 DTO로 변환하여 반환</li>
	 * </ol>
	 * 
	 * @param proposalNo 제안서 식별자 (요청 경로 변수, 예: "F00001")
	 * @param request 파일 메타데이터 (userId, companyName 등)
	 * @param file 업로드할 파일 (MultipartFile)
	 * @return 저장된 파일 정보를 담은 응답 DTO
	 * @throws IOException S3 업로드 중 I/O 오류 발생 시
	 */
	public ProposalFileResponseDto fileUpload(String proposalNo, ProposalFileRequestDto request, MultipartFile file)
			throws IOException {

		// 1. 요청 DTO 유효성 확보: null이면 빈 객체 생성
        ProposalFileRequestDto effectiveRequest = request != null ? request : new ProposalFileRequestDto();
        
        // 2. userId 자동 채움: 요청에 userId가 없으면 현재 인증된 사용자 ID로 설정
        //    - Spring Security의 SecurityContextHolder에서 인증 정보 추출
        //    - 로그인하지 않았거나 인증 실패 시 null 반환
        if (!StringUtils.hasText(effectiveRequest.getUserId())) {
            String currentUserId = resolveCurrentUserId();
            if (StringUtils.hasText(currentUserId)) {
                effectiveRequest.setUserId(currentUserId);
            }
        }

		// 3. S3 업로드 경로 정규화
		//    - 현재 정책: 모든 파일은 document/ 루트에 저장
		//    - 향후 요구사항에 따라 요청에서 전달된 filePath로 분기 가능
		String filePath = proposalNo; // 현재는 proposalNo를 filePath로 사용 (확장 시 분리 가능)
		String uploadDirectory = resolveUploadDirectory(filePath);
		
		// 4. S3에 파일 업로드
		//    - 랜덤 UUID 기반 파일명 생성
		//    - 업로드 결과: S3 객체 키(key)와 공개 URL(url) 반환
		S3StorageService.UploadResult result = s3StorageService.upload(uploadDirectory, file);

		// 5. 엔티티 생성 (MapStruct 매퍼 사용)
		//    - proposalNo, request, 원본 파일명, S3 업로드 결과, 현재 시각을 조합
		//    - pr_no, pr_user_id, pr_file_name, pr_file_object_key, pr_file_storage_url 등 매핑
		ProposalFileEntity entity = proposalFileEntityMapper.toEntity(proposalNo, effectiveRequest, file.getOriginalFilename(),
				result, LocalDateTime.now());

		// 6. DB에 파일 메타데이터 저장
		//    - MyBatis: INSERT INTO proposal_file (...) VALUES (...)
		//    - pr_no는 자동 생성 (F00001, F00002, ...)
		proposalFileMapper.insertFile(entity);
		
		// 7. 저장된 엔티티를 응답 DTO로 변환하여 반환
		return proposalFileEntityMapper.toDto(entity);
	}

	/**
     * 다건 업로드:
     * - 하나라도 실패하면 전체 실패
     * - DB는 트랜잭션 롤백
     * - S3는 이미 업로드된 것들 전부 삭제(보상처리)
     */
	@Transactional(rollbackFor = Exception.class)
    public List<ProposalFileResponseDto> fileUploadListStrict(String proposalNo,ProposalFileRequestDto request,List<MultipartFile> files) throws IOException {
		List<String> uploadfileObjKeys = new ArrayList<>();
		List<ProposalFileResponseDto> results = new ArrayList<>();

		try{
			for(MultipartFile file : files){
				String uploadDir = resolveUploadDirectory(proposalNo);
				S3StorageService.UploadResult s3Result = s3StorageService.upload(uploadDir, file);
				
				uploadfileObjKeys.add(s3Result.key());
				ProposalFileEntity entity = proposalFileEntityMapper.toEntity(
                proposalNo, request, file.getOriginalFilename(), s3Result, LocalDateTime.now());
				proposalFileMapper.insertFile(entity);
				results.add(proposalFileEntityMapper.toDto(entity));
			}
			return results;	
		}
		//예외 발생시 S3에서 업로드된 파일 삭제
		catch(Exception e){
			for(String key : uploadfileObjKeys){
				try {
					s3StorageService.delete(key);
				} catch (Exception s3Ex) {   // 삭제 실패 시 로그만 남기고 계속 진행 (이미 메인 예외가 발생한 상태)
                	log.error("S3 rollback failed for key: {}", key, s3Ex);
				}
			}
			throw e;
		}
		
	}

	/**
	 * S3 업로드 디렉터리 경로 정규화
	 * 
	 * <p>이 메서드는 현재 정책에 따라 모든 파일을 "document" 디렉터리에 저장합니다.</p>
	 * 
	 * <h4>현재 동작</h4>
	 * <ul>
	 *   <li>모든 파일 경로(filePath)에 대해 "document" 문자열만 반환</li>
	 *   <li>앞으로 카테고리/제안서별 하위 폴더가 필요하면 이 메서드를 확장</li>
	 * </ul>
	 * 
	 * @param filePath 업로드 요청에서 전달된 참조 값 (현재 단계에서는 사용하지 않음)
	 * @return 정규화된 S3 업로드 디렉터리 경로 (항상 "document")
	 */
    private String resolveUploadDirectory(String filePath) {
		String dataType = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        return "document/" + dataType;
    }

	/**
	 * 현재 인증된 사용자 ID 추출
	 * 
	 * <p>Spring Security의 SecurityContextHolder에서 현재 요청의 인증 정보를 가져와
	 * 사용자 ID를 추출합니다. JWT 인증 필터(JwtAuthFilter)가 설정한 Authentication 객체를 사용합니다.</p>
	 * 
	 * <h4>추출 우선순위</h4>
	 * <ol>
	 *   <li>UserDetails 인터페이스 구현체 → getUsername() 호출</li>
	 *   <li>String principal (JWT 인증 시 userId가 직접 principal로 설정됨) → 그대로 반환</li>
	 *   <li>그 외 → authentication.getName() 호출</li>
	 * </ol>
	 * 
	 * <h4>반환값</h4>
	 * <ul>
	 *   <li>인증된 사용자 ID (예: "user123", "dudgns1172")</li>
	 *   <li>인증되지 않았거나 익명 사용자인 경우 null</li>
	 * </ul>
	 * 
	 * @return 현재 로그인한 사용자의 ID, 인증되지 않았으면 null
	 */
    private String resolveCurrentUserId() {
    	// 1. SecurityContextHolder에서 현재 요청의 인증 정보 가져오기
    	//    - JwtAuthFilter가 JWT 토큰 검증 후 설정한 Authentication 객체
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // 2. 인증 정보가 없거나 인증되지 않은 경우 null 반환
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

		// 3. Principal 객체에서 사용자 ID 추출
        Object principal = authentication.getPrincipal();
        
        // 3-1. UserDetails 인터페이스 구현체인 경우 (일반적인 Spring Security 사용 패턴)
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

		// 3-2. String principal인 경우 (JWT 인증 시 userId가 직접 설정됨)
		//      - JwtAuthFilter에서 UsernamePasswordAuthenticationToken(userId, null, authorities) 형태로 설정
		//      - "anonymousUser"는 제외 (Spring Security 기본 익명 사용자)
        if (principal instanceof String principalString && !"anonymousUser".equalsIgnoreCase(principalString)) {
            return principalString;
        }

		// 3-3. 그 외의 경우 authentication.getName() 반환
        return authentication.getName();
    }

	/**
	 * 제안서 파일 목록 조회
	 * 
	 * <p>특정 제안서(proposalNo)에 속한 모든 파일의 메타데이터를 DB에서 조회하여 반환합니다.
	 * 읽기 전용 트랜잭션으로 실행되어 성능을 최적화합니다.</p>
	 * 
	 * <h4>처리 흐름</h4>
	 * <ol>
	 *   <li>MyBatis 매퍼로 DB 조회: SELECT * FROM proposal_file WHERE pr_no = #{proposalNo}</li>
	 *   <li>조회된 엔티티 리스트를 MapStruct로 DTO 리스트로 변환</li>
	 *   <li>DTO 리스트 반환</li>
	 * </ol>
	 * 
	 * @param proposalNo 제안서 식별자 (예: "F00001")
	 * @return 해당 제안서에 속한 파일 목록 (ProposalFileResponseDto 리스트)
	 */
	@Transactional(readOnly = true)
	public List<ProposalFileResponseDto> list(String proposalNo) {
		// 1. DB에서 제안서 번호로 파일 엔티티 목록 조회
		//    - MyBatis: SELECT * FROM proposal_file WHERE pr_no = #{proposalNo} ORDER BY created_at DESC
		List<ProposalFileEntity> entities = proposalFileMapper.findByProposalNo(proposalNo);
		
		// 2. 엔티티 리스트를 DTO 리스트로 변환 (MapStruct)
		return proposalFileEntityMapper.fileDtoList(entities);
	}

	/**
	 * 제안서 파일 삭제
	 * 
	 * <p>특정 제안서(proposalNo)의 특정 파일(fileId)을 S3와 DB에서 모두 삭제합니다.
	 * 삭제 전에 파일이 실제로 해당 제안서에 속하는지 검증합니다.</p>
	 * 
	 * <h4>처리 흐름</h4>
	 * <ol>
	 *   <li>DB에서 파일 조회 및 검증 (proposalNo와 fileId 모두 일치해야 함)</li>
	 *   <li>파일이 없으면 404 예외 발생</li>
	 *   <li>S3에서 파일 삭제 (S3 객체 키 또는 URL 사용)</li>
	 *   <li>DB에서 파일 메타데이터 삭제</li>
	 * </ol>
	 * 
	 * @param proposalNo 제안서 번호 (예: PR-0001)
	 * @param fileId 파일 ID (DB의 pr_file_id)
	 * @throws ResponseStatusException 파일을 찾을 수 없거나 제안서 번호가 일치하지 않을 때 404 반환
	 */
	public void delete(String proposalNo, Long fileId) {
		// 1. DB에서 파일 조회 및 검증
		//    - MyBatis: SELECT * FROM proposal_file WHERE pr_file_id = #{fileId} AND pr_no = #{proposalNo}
		//    - Optional.orElseThrow()로 파일이 없으면 404 예외 발생
		ProposalFileEntity entity = proposalFileMapper.findByIdAndProposalNo(fileId, proposalNo)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));

		// 2. S3에서 파일 삭제
		//    - S3StorageService.delete()는 URL 또는 객체 키를 받아 S3 객체 삭제
		//    - 삭제 실패 시 예외 발생 (S3StorageService에서 처리)
		s3StorageService.delete(entity.getPr_file_storage_url());
		
		// 3. DB에서 파일 메타데이터 삭제
		//    - MyBatis: DELETE FROM proposal_file WHERE pr_file_id = #{fileId}
		proposalFileMapper.delete(fileId);
	}

	@Transactional(readOnly = true)
	public String getDownloadUrl(Long fileId) {
		ProposalFileEntity entity = proposalFileMapper.findById(fileId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found"));
		return entity.getPr_file_storage_url();
	}
}