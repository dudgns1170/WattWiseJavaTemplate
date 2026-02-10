package com.app.controller;

import com.app.common.ApiResponse;
import com.app.service.S3StorageService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

/**
 * 공용 S3 파일 관리 REST API 컨트롤러
 * 
 * <p>이 컨트롤러는 AWS S3 스토리지에 대한 범용 파일 작업을 제공합니다.
 * DB에 메타데이터를 저장하지 않고, 순수하게 S3 업로드/다운로드/삭제 기능만 수행합니다.</p>
 * 
 * <h3>주요 차이점: S3Controller vs ProposalFileController</h3>
 * <ul>
 *   <li><b>S3Controller (/api/file/*)</b>: 
 *       <ul>
 *         <li>범용 S3 파일 업로드/다운로드/삭제 API</li>
 *         <li>DB에 메타데이터를 저장하지 않음</li>
 *         <li>순수하게 S3 작업만 수행</li>
 *         <li>임시 파일, 이미지, 공용 자료 등에 사용</li>
 *       </ul>
 *   </li>
 *   <li><b>ProposalFileController (/api/proposals/{proposalNo}/files/*)</b>: 
 *       <ul>
 *         <li>제안서 전용 파일 관리 API</li>
 *         <li>S3 업로드 후 파일 정보를 proposal_file 테이블에 저장</li>
 *         <li>제안서 번호로 파일 목록 조회/삭제 가능</li>
 *         <li>비즈니스 로직과 데이터 관리가 필요한 경우에 사용</li>
 *       </ul>
 *   </li>
 * </ul>
 * 
 * <h3>인증 요구사항</h3>
 * <p>이 컨트롤러의 모든 엔드포인트는 인증이 필요하지 않습니다 (SecurityConfig에서 permitAll 설정).
 * 공개적으로 접근 가능한 API이므로, 보안이 필요한 파일은 ProposalFileController를 사용하세요.</p>
 * 
 * @see com.app.service.S3StorageService
 * @see com.app.controller.ProposalFileController
 */
@Hidden
@RestController
@RequestMapping("/api/file")
@Tag(name = "공용 파일", description = "공용 S3 파일 업로드/조회/삭제 API")
public class S3Controller {
	// S3 스토리지 서비스: AWS S3 업로드/다운로드/삭제 기능 제공
    private final S3StorageService s3;

    public S3Controller(S3StorageService s3) {
        this.s3 = s3;
    }

	/**
	 * 공용 S3 파일 업로드 API
	 * 
	 * <p>S3에 파일을 업로드하고, S3 객체 키와 공개 URL을 반환합니다.
	 * DB에 메타데이터를 저장하지 않으므로, 임시 파일이나 공용 자료 업로드에 적합합니다.</p>
	 * 
	 * <h4>요청 형식</h4>
	 * <pre>
	 * POST /api/file/upload
	 * Content-Type: multipart/form-data
	 * 
	 * - file: (필수) 업로드할 파일
	 * - dir: (선택) S3 디렉터리 경로 (예: "images", "documents")
	 * </pre>
	 * 
	 * <h4>응답 예시</h4>
	 * <pre>
	 * HTTP 200 OK
	 * {
	 *   "key": "images/abc123-sample.jpg",
	 *   "url": "https://s3.amazonaws.com/wattwise-dev-tset/images/abc123-sample.jpg"
	 * }
	 * </pre>
	 * 
	 * @param dir S3 디렉터리 경로 (선택, 예: "images")
	 * @param file 업로드할 파일 (multipart/form-data의 "file" 파트)
	 * @return S3 객체 키(key)와 공개 URL(url)을 포함한 Map
	 * @throws Exception S3 업로드 중 오류 발생 시
	 */
    @Operation(summary = "공용 파일 업로드", description = "S3에 공용 파일을 업로드하고 URL 반환")
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse> upload(@RequestParam(value = "dir", required = false) String dir,
                                    @RequestPart("file") MultipartFile file) throws Exception {
    	// 1. S3에 파일 업로드
    	//    - dir이 있으면 {dir}/{랜덤파일명}, 없으면 {랜덤파일명}
    	//    - 랜덤 UUID 기반 파일명 생성
        S3StorageService.UploadResult result = s3.upload(dir, file);
        
        // 2. S3 객체 키와 공개 URL을 공통 응답 포맷으로 반환
        ApiResponse body = ApiResponse.ok(Map.of("key", result.key(), "url", result.url()));
        return ResponseEntity.ok(body);
    }

	/**
	 * S3 파일 Presigned GET URL 생성 API
	 * 
	 * <p>S3 버킷이 private일 때, 임시로 접근 가능한 Presigned URL을 생성합니다.
	 * 생성된 URL은 지정된 시간(기본 5분) 동안만 유효하며, 그 이후에는 접근할 수 없습니다.</p>
	 * 
	 * <h4>요청 형식</h4>
	 * <pre>
	 * GET /api/file/presign-get?key={s3ObjectKey}&ttlMinutes={minutes}
	 * 
	 * - key: (필수) S3 객체 키 (예: "images/abc123-sample.jpg")
	 * - ttlMinutes: (선택) URL 유효 시간(분), 기본값 5분
	 * </pre>
	 * 
	 * <h4>응답 예시</h4>
	 * <pre>
	 * HTTP 200 OK
	 * {
	 *   "url": "https://s3.amazonaws.com/wattwise-dev-tset/images/abc123-sample.jpg?X-Amz-Algorithm=..."
	 * }
	 * </pre>
	 * 
	 * @param key S3 객체 키 (필수, 예: "images/abc123-sample.jpg")
	 * @param ttlMinutes URL 유효 시간(분, 선택, 기본 5분)
	 * @return Presigned GET URL을 포함한 Map
	 */
    @Operation(summary = "공용 파일 Presigned GET URL 생성", description = "S3에서 공용 파일 Presigned GET URL 생성")
    @GetMapping("/presign-get")
    public ResponseEntity<ApiResponse> presignGet(@RequestParam("key") String key,
                                        @RequestParam(value = "ttlMinutes", required = false) Long ttlMinutes) {
    	// 1. TTL 설정: 지정되지 않으면 기본 5분
        Duration ttl = ttlMinutes != null ? Duration.ofMinutes(ttlMinutes) : Duration.ofMinutes(5);
        
        // 2. S3 Presigned GET URL 생성
        //    - AWS SDK의 S3Presigner 사용
        //    - 생성된 URL은 TTL 동안만 유효
        URL url = s3.createPresignedGetUrl(key, ttl);
        
        // 3. Presigned URL을 공통 응답 포맷으로 반환
        ApiResponse body = ApiResponse.ok(Map.of("url", url.toString()));
        return ResponseEntity.ok(body);
    }

	/**
	 * S3 파일 삭제 API
	 * 
	 * <p>S3에서 파일을 삭제합니다. DB에 메타데이터를 저장하지 않으므로,
	 * 순수하게 S3 객체만 삭제합니다.</p>
	 * 
	 * <h4>요청 형식</h4>
	 * <pre>
	 * DELETE /api/file?key={s3ObjectKey}
	 * 
	 * - key: (필수) S3 객체 키 (예: "images/abc123-sample.jpg")
	 * </pre>
	 * 
	 * <h4>응답 예시</h4>
	 * <pre>
	 * HTTP 204 No Content
	 * (응답 본문 없음)
	 * </pre>
	 * 
	 * @param key S3 객체 키 (필수, 예: "images/abc123-sample.jpg")
	 * @return HTTP 204 No Content 상태 (본문 없음)
	 */
    @Operation(summary = "공용 파일 삭제", description = "S3에서 공용 파일을 삭제")
    @DeleteMapping
    public ResponseEntity<ApiResponse> delete(@RequestParam("key") String key) {
    	// 1. S3에서 파일 삭제
    	//    - AWS SDK의 S3Client.deleteObject() 사용
    	//    - 삭제 실패 시 예외 발생 (S3StorageService에서 처리)
        s3.delete(key);
        
        // 2. HTTP 200 OK + 공통 응답 포맷 반환 (삭제 성공)
        ApiResponse body = ApiResponse.success(HttpStatus.OK.value(), "파일 삭제 완료", null);
        return ResponseEntity.ok(body);
    }
}
