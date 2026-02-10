---
trigger: always_on
---

---
trigger: always_on
description: WattWise Java Backend architecture 가이드
globs: "src/main/java/**/*.java", "src/main/resources/**/*.yml"
---

# 1. 개요 (Overview)

- **[아키텍처 스타일]**
  - Spring Boot + MyBatis 기반 **3-Layered Architecture** 사용
  - 표현 계층: `controller`
  - 비즈니스 계층: `service`
  - 데이터 접근 계층: `repository` / `mapper`
- **[주요 도메인]**
  - 인증/인가: `AuthController`, `AuthService`, `JwtService`, Redis (Refresh Token)
  - 사용자: `UserController`, `UserService`
  - 메일 인증: `MailController`
  - 파일: `S3Controller`, `ProposalFileController`, `ProposalFileService`, `S3StorageService`
  - RTU 시뮬레이터: `RtuSimulatorController`

---

# 2. 패키지 구조 원칙

- **[루트 패키지]**
  - 기본 패키지: `com.app`
- **[레이어 기준 패키지]**
  - `com.app.config` : 애플리케이션 설정 (AppProps, CORS, Security, AWS, Swagger 등)
  - `com.app.security` : 인증/인가, JWT, 필터, `SecurityConfig`
  - `com.app.controller` : REST API 엔드포인트, 요청/응답 매핑
  - `com.app.service` : 비즈니스 로직, 트랜잭션 경계
  - `com.app.repository` : MyBatis 매퍼 인터페이스
  - `com.app.mapper` : Entity ↔ DTO 변환 (MapStruct 등)
  - `com.app.entity` : 도메인/DB 엔티티
  - `com.app.dto` : API 요청/응답 DTO
  - `com.app.common` : 공통 유틸, 공통 응답/에러 포맷 등
- **[도메인 기준 하위 패키지 (선택)]**
  - 규모가 커질 경우 도메인 단위 하위 패키지를 사용한다.
  - 예: `com.app.auth.controller`, `com.app.auth.service`, `com.app.proposal.service`, `com.app.proposal.dto`

---

# 3. 레이어별 책임

- **[controller]**
  - HTTP 요청/응답 매핑, 경로/메서드 정의
  - `@RequestBody`, `@PathVariable`, `@RequestParam`, `@RequestPart` 등으로 파라미터 수신
  - 요청/응답은 항상 DTO(`com.app.dto`)를 통해 처리
  - 비즈니스 로직은 `service` 계층에 위임
  - `ResponseEntity<T>` 를 사용해 상태 코드/헤더/바디를 명시적으로 제어
- **[service]**
  - 도메인 규칙, 유즈케이스 구현 (예: 로그인, 토큰 갱신, 제안서 파일 업로드)
  - 트랜잭션 경계 (`@Transactional`)는 기본적으로 service 계층에만 선언
  - `repository` / `mapper`를 사용해 데이터 읽기·쓰기
  - 여러 도메인/외부 시스템(DB, Redis, S3 등)을 조합하는 오케스트레이션 담당
- **[repository / mapper]**
  - 데이터 접근 전용 계층 (MyBatis 매퍼)
  - 쿼리 작성, 데이터 저장/조회 담당
  - 비즈니스 규칙은 포함하지 않음
- **[entity]**
  - JPA/DB 엔티티, 값 객체 정의
  - Web, HTTP, Controller, Service에 직접 의존하지 않음
  - 도메인 의미가 있는 메서드만 포함 (가급적 단순 데이터 홀더)
- **[dto / mapper]**
  - DTO: 요청/응답 모델, 검증/직렬화 담당
  - Mapper: Entity ↔ DTO 변환을 일관되게 담당 (Controller/Service에 변환 로직을 흩뿌리지 않음)

---

# 4. 의존성 규칙

- **[허용되는 방향]**
  - `controller` → `service` → `repository` / `mapper` → `entity`
  - `controller` → `dto`, `common`
  - `service` → `dto`, `mapper`, `entity`, `repository`, `common`, `security` (토큰 파싱 등)
  - `repository` / `mapper` → `entity`, `common`
- **[금지되는 방향]**
  - `repository` → `service` 의존 금지
  - `service` → `controller` 의존 금지
  - `entity` → `controller`, `service`, `repository`, `security` 의존 금지
- **[도메인 간 의존]**
  - 다른 도메인에 직접 강하게 의존하지 않고, 필요한 경우 service 계층에서 조합
  - 예: Auth 도메인이 Proposal 도메인을 직접 참조하기보다, 상위 서비스/유스케이스에서 묶어 사용

---

# 5. 트랜잭션 및 상태 관리

- **[트랜잭션]**
  - 비즈니스 트랜잭션 경계는 `service` 계층에서 `@Transactional`로 관리
  - 읽기 전용 조회는 `@Transactional(readOnly = true)`를 사용해 의도를 드러냄
- **[상태 관리]**
  - 서비스 빈은 가능한 **stateless**하게 유지 (요청 간 공유 mutable 필드 금지)
  - 상태는 DB(MySQL), Redis, S3, DynamoDB 등 외부 스토어에 저장
  - JWT Access Token은 stateless하게 처리하고, Refresh Token/세션 상태는 Redis에 저장
- **[외부 시스템 연동]**
  - DB: MySQL (사용자, 제안서 파일 등 도메인 데이터)
  - Redis: Refresh Token, 이메일 인증 코드 등 TTL이 분명한 상태
  - S3: 파일 저장소, URL/프리사인 URL 관리
  - RTU/DynamoDB: 정산 데이터 전송 (시뮬레이터)

---

# 6. API–도메인 경계

- **[DTO 경계]**
  - Controller는 엔티티를 직접 반환하지 않고, 항상 Response DTO를 반환
  - 요청 바디는 Entity가 아닌 Request DTO로 받는 것을 원칙으로 함
  - 외부 API 스펙(요청/응답)이 변경되어도 내부 도메인 모델이 바로 영향을 받지 않도록 DTO로 경계를 분리
- **[문서와 코드의 일관성]**
  - [BACKEND_API_AND_STRUCTURE.md](cci:7://file:///d:/WattWiseJava/BACKEND_API_AND_STRUCTURE.md:0:0-0:0)의 엔드포인트/요청/응답 예시를 기준으로  
    `controller`와 DTO 구조를 설계하고 유지한다.
  - Swagger/OpenAPI 적용 시, 이 문서를 기준으로 `@Tag`, `@Operation`, `@Schema`를 부여한다.

---

# 7. 설정 및 환경 분리

- **[환경 설정 파일]**
  - `src/main/resources/application.yml` : 기본 설정
  - 필요 시 `application-local.yml`, `application-prod.yml` 등 프로파일별 분리
- **[환경 변수]**
  - [.env](cci:7://file:///d:/WattWiseJava/.env:0:0-0:0) 파일에 DB, JWT, Redis, AWS S3, Mail 등 민감 정보를 정의하고  
    `application.yml`에서 `${...}` 형태로 참조
- **[보안]**
  - JWT, DB 패스워드, AWS 키 등은 git에 커밋하지 않고,  
    [.env](cci:7://file:///d:/WattWiseJava/.env:0:0-0:0) / 환경 변수 / 서버 설정으로만 관리한다.