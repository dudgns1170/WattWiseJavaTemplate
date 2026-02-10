package com.app.controller;

import com.app.common.ApiResponse;
import com.app.dto.ProposalFileRequestDto;
import com.app.dto.ProposalFileResponseDto;
import com.app.service.ProposalFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 제안서(Proposal) 파일 관리 전용 REST API 컨트롤러
 * 
 * <p>이 컨트롤러는 특정 제안서(proposalNo)에 연결된 파일들을 관리합니다.
 * 공용 S3Controller와 달리, 제안서 컨텍스트 내에서 파일 메타데이터를 DB에 저장하고
 * 제안서 번호별로 파일을 조회/삭제할 수 있는 비즈니스 로직을 제공합니다.</p>
 * 
 * <h3>주요 차이점: S3Controller vs ProposalFileController</h3>
 * <ul>
 *   <li><b>S3Controller (/api/file/*)</b>: 범용 S3 파일 업로드/다운로드/삭제 API. 
 *       DB에 메타데이터를 저장하지 않고 순수하게 S3 작업만 수행합니다.</li>
 *   <li><b>ProposalFileController (/api/proposals/{proposalNo}/files/*)</b>: 제안서 전용 파일 관리 API. 
 *       S3 업로드 후 파일 정보를 proposal_file 테이블에 저장하고, 
 *       제안서 번호로 파일 목록을 조회하거나 삭제할 수 있습니다.</li>
 * </ul>
 * 
 * <h3>인증 요구사항</h3>
 * <p>이 컨트롤러의 모든 엔드포인트는 JWT 인증이 필요합니다 (SecurityConfig 참조).
 * 요청 시 반드시 <code>Authorization: Bearer {accessToken}</code> 헤더를 포함해야 합니다.</p>
 * 
 * @see com.app.service.ProposalFileService
 * @see com.app.controller.S3Controller
 */
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "제안서 파일", description = "제안서별 파일 업로드/조회/삭제 API")
public class ProposalFileController {

	// ProposalFileService: 실제 비즈니스 로직(S3 업로드, DB 저장, 조회, 삭제)을 처리하는 서비스
	private final ProposalFileService proposalFileService;

	/**
	 * 제안서 파일 업로드 API
	 * 
	 * <p>특정 제안서(proposalNo)에 파일을 업로드하고, 파일 메타데이터를 DB에 저장합니다.
	 * S3에는 <code>document/{proposalNo}/{랜덤파일명}</code> 형태로 저장되며,
	 * DB에는 파일명, S3 URL, 사용자 정보 등이 함께 기록됩니다.</p>
	 * 
	 * <h4>요청 형식</h4>
	 * <pre>
	 * POST /api/proposals/{proposalNo}/files/upload
	 * Content-Type: multipart/form-data
	 * Authorization: Bearer {accessToken}
	 * 
	 * - file: (필수) 업로드할 파일
	 * - requestMeta: (선택) JSON 형태의 메타데이터
	 *   {
	 *     "userId": "user123",           // 생략 시 로그인 사용자 ID 자동 채움
	 *     "companyName": "회사명",
	 *     "siteAddress": "설치 주소",
	 *     "capacity": "용량",
	 *     "createdByName": "작성자명",
	 *     "updatedByName": "수정자명"
	 *   }
	 * </pre>
	 * 
	 * <h4>응답 예시</h4>
	 * <pre>
	 * HTTP 201 Created
	 * Location: /api/proposals/F00001/files/sample.pdf
	 * 
	 * {
	 *   "proposalNo": "F00001",
	 *   "userId": "user123",
	 *   "companyName": "회사명",
	 *   "fileName": "abc123-sample.pdf",
	 *   "fileUrl": "https://s3.amazonaws.com/.../document/F00001/abc123-sample.pdf",
	 *   "createdName": "홍길동",
	 *   "createdDt": "2025-11-05T12:00:00"
	 * }
	 * </pre>
	 * 
	 * @param proposalNo 제안서 식별자 (경로 변수, 예: "F00001")
	 * @param file 업로드할 파일 (multipart/form-data의 "file" 파트)
	 * @param requestMeta 파일 메타데이터 (선택, multipart/form-data의 "requestMeta" 파트, JSON)
	 * @return 저장된 파일 정보를 담은 DTO와 HTTP 201 Created 상태
	 * @throws IOException S3 업로드 중 I/O 오류 발생 시
	 * @throws IllegalArgumentException 파일이 null이거나 비어있을 때
	 */
	@Operation(summary = "제안서 파일 업로드", description = "특정 제안서에 파일을 업로드하고 메타데이터를 저장")
	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse> upload(@RequestParam("proposalNo") String proposalNo,
			@RequestPart("file") MultipartFile file,
			@RequestPart(value = "requestMeta", required = false) ProposalFileRequestDto requestMeta)
			throws IOException {
		// 1. 파일 유효성 검증: 파일이 비어있으면 예외 발생
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File must not be empty");
		}

		// 2. requestMeta가 null이면 빈 DTO 생성 (서비스에서 userId 자동 채움)
		ProposalFileRequestDto safeRequest = requestMeta != null ? requestMeta : new ProposalFileRequestDto();

		// 3. 서비스 호출: S3 업로드 + DB 저장
		//    - S3 경로: document/{proposalNo}/{랜덤파일명}
		//    - DB: proposal_file 테이블에 메타데이터 INSERT
		//    - userId가 비어있으면 현재 로그인 사용자 ID로 자동 채움
		ProposalFileResponseDto saved = proposalFileService.fileUpload(proposalNo, safeRequest, file);
		
		// 4. Location 헤더 생성: 업로드된 파일의 리소스 URI
		URI location = UriComponentsBuilder.fromPath("/api/proposals/{proposalNo}/files/{fileName}")
				.buildAndExpand(proposalNo, saved.getFileName())
				.encode()
				.toUri();
		// 5. HTTP 201 Created 응답 반환 (Location 헤더 + 공통 응답 DTO)
		ApiResponse body = ApiResponse.created(saved);
		return ResponseEntity.created(location).body(body);
	}
	/*
	 * @param proposalNo 제안서 식별자 (경로 변수, 예: "F00001")
	 * @param file 업로드할 파일 (multipart/form-data의 "file" 파트)
	 * @param requestMeta 파일 메타데이터 (선택, multipart/form-data의 "requestMeta" 파트, JSON)
	 * @return 저장된 파일 정보를 담은 DTO와 HTTP 201 Created 상태
	 * @throws IOException S3 업로드 중 I/O 오류 발생 시
	 * @throws IllegalArgumentException 파일이 null이거나 비어있을 때
	 */
	@Operation(summary = "다중 업로드", description = "특정 제안서에 파일을 업로드하고 메타데이터를 저장")
	@PostMapping(value = "/fileUploadListStrict", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse> fileUploadListStrict(@RequestParam("proposalNo") String proposalNo,@RequestPart("file") List<MultipartFile> files,
			@RequestPart(value = "requestMeta", required = false) ProposalFileRequestDto requestMeta)
			throws IOException {
		// 1. 파일 유효성 검증: 파일이 비어있으면 예외 발생
		if (files == null || files.isEmpty()) {
			throw new IllegalArgumentException("File must not be empty");
		}
		for(MultipartFile f: files) {
			if (f == null || f.isEmpty()) {
				throw new IllegalArgumentException("File must not be empty");
			}
		}

		// 2. requestMeta가 null이면 빈 DTO 생성 (서비스에서 userId 자동 채움)
		ProposalFileRequestDto safeRequest = requestMeta != null ? requestMeta : new ProposalFileRequestDto();

		// 3. 서비스 호출: S3 업로드 + DB 저장 (트랜잭션 보장을 위해 리스트 전체를 서비스에 위임)
		//    - 서비스 내부에서 루프를 돌며 처리하며, 하나라도 실패 시 전체 롤백됨
		List<ProposalFileResponseDto> savedList = proposalFileService.fileUploadListStrict(proposalNo, safeRequest, files);
		
		// 4. Location 헤더 생성: 업로드된 파일 목록을 확인할 수 있는 리소스 URI
		URI location = UriComponentsBuilder.fromPath("/api/files/list")
				.queryParam("proposalNo", proposalNo)
				.build()
				.encode()
				.toUri();

		// 5. HTTP 201 Created 응답 반환 (Location 헤더 + 저장된 파일 목록 DTO)
		ApiResponse body = ApiResponse.created(savedList);
		return ResponseEntity.created(location).body(body);
	}

	/**
	 * 제안서 파일 목록 조회 API
	 * 
	 * <p>특정 제안서(proposalNo)에 연결된 모든 파일의 메타데이터를 조회합니다.
	 * DB의 proposal_file 테이블에서 해당 제안서 번호로 필터링하여 반환합니다.</p>
	 * 
	 * <h4>요청 형식</h4>
	 * <pre>
	 * GET /api/proposals/{proposalNo}/files
	 * Authorization: Bearer {accessToken}
	 * </pre>
	 * 
	 * <h4>응답 예시</h4>
	 * <pre>
	 * HTTP 200 OK
	 * 
	 * [
	 *   {
	 *     "proposalNo": "F00001",
	 *     "userId": "user123",
	 *     "fileName": "abc123-sample.pdf",
	 *     "fileUrl": "https://s3.amazonaws.com/.../document/F00001/abc123-sample.pdf",
	 *     "createdDt": "2025-11-05T12:00:00"
	 *   },
	 *   {
	 *     "proposalNo": "F00002",
	 *     "userId": "user456",
	 *     "fileName": "def456-report.xlsx",
	 *     "fileUrl": "https://s3.amazonaws.com/.../document/F00001/def456-report.xlsx",
	 *     "createdDt": "2025-11-04T10:30:00"
	 *   }
	 * ]
	 * </pre>
	 * 
	 * @param proposalNo 제안서 식별자 (경로 변수, 예: "F00001")
	 * @return 해당 제안서에 속한 파일 목록 (DTO 리스트)과 HTTP 200 OK 상태
	 */
	@Operation(summary = "제안서 파일 목록 조회", description = "제안서 번호로 연결된 파일 목록을 조회")
	@GetMapping("/list")
	public ResponseEntity<ApiResponse> list(@RequestParam("proposalNo") String proposalNo) {
		// 1. 서비스 호출: DB에서 proposalNo로 파일 목록 조회
		//    - MyBatis: SELECT * FROM proposal_file WHERE pr_no = #{proposalNo}
		List<ProposalFileResponseDto> files = proposalFileService.list(proposalNo);
		
		// 2. HTTP 200 OK 응답 반환 (공통 응답 DTO)
		ApiResponse body = ApiResponse.ok(files);
		return ResponseEntity.ok(body);
	}

	/**
	 * 제안서 파일 삭제 API
	 * 
	 * <p>특정 제안서(proposalNo)의 특정 파일(fileId)을 S3와 DB에서 모두 삭제합니다.
	 * 삭제 전에 해당 파일이 실제로 해당 제안서에 속하는지 검증합니다.</p>
	 * 
	 * <h4>요청 형식</h4>
	 * <pre>
	 * DELETE /api/proposals/{proposalNo}/files/{fileId}
	 * Authorization: Bearer {accessToken}
	 * </pre>
	 * 
	 * <h4>응답 예시</h4>
	 * <pre>
	 * HTTP 204 No Content
	 * (응답 본문 없음)
	 * </pre>
	 * 
	 * <h4>에러 응답</h4>
	 * <pre>
	 * HTTP 404 Not Found
	 * {
	 *   "status": 404,
	 *   "error": "Not Found",
	 *   "message": "File not found"
	 * }
	 * </pre>
	 * 
	 * @param proposalNo 제안서 번호 (경로 변수, 예: PR-0001)
	 * @param fileId 파일 ID (경로 변수, DB의 pr_file_id)
	 * @return HTTP 204 No Content 상태 (본문 없음)
	 * @throws ResponseStatusException 파일을 찾을 수 없거나 제안서 번호가 일치하지 않을 때 404 반환
	 */
	@Operation(summary = "제안서 파일 삭제", description = "특정 제안서에 속한 파일을 S3와 DB에서 삭제")
	@DeleteMapping("/{fileId}")
	public ResponseEntity<ApiResponse> delete(@RequestParam("proposalNo") String proposalNo,
			@PathVariable("fileId") Long fileId) {
		// 1. 서비스 호출: 파일 존재 여부 확인 + S3 삭제 + DB 삭제
		//    - DB 조회: SELECT * FROM proposal_file WHERE pr_file_id = #{fileId} AND pr_no = #{proposalNo}
		//    - 없으면 404 예외 발생
		//    - S3 삭제: s3StorageService.delete(fileUrl)
		//    - DB 삭제: DELETE FROM proposal_file WHERE pr_file_id = #{fileId}
		proposalFileService.delete(proposalNo, fileId);
		
		// 2. HTTP 200 OK + 공통 응답 포맷 반환 (삭제 성공)
		ApiResponse body = ApiResponse.success(HttpStatus.OK.value(), "파일 삭제 완료", null);
		return ResponseEntity.ok(body);
	}

	@Operation(summary = "제안서 파일 다운로드", description = "fileId로 다운로드 URL 조회")
	@GetMapping("/download/{fileId}")
	public ResponseEntity<ApiResponse> download(@PathVariable("fileId") Long fileId) {
		String url = proposalFileService.getDownloadUrl(fileId);
		ApiResponse body = ApiResponse.ok(Map.of("url", url));
		return ResponseEntity.ok(body);
	}
}