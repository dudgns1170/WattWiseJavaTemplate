# WattWise Backend Architecture

WattWise 백엔드 애플리케이션의 아키텍처 문서입니다.

---

## 1. 개요

- **프레임워크**: Spring Boot 3.x
- **언어**: Java 17+
- **빌드 도구**: Gradle
- **데이터베이스**: MySQL (MyBatis)
- **캐시/세션**: Redis
- **파일 스토리지**: AWS S3
- **API 문서**: Swagger (OpenAPI 3.0)

---

## 2. 아키텍처 스타일

**3-Layered Architecture** 기반으로 구성되어 있습니다.

```
┌─────────────────────────────────────────────────────────────┐
│                      Presentation Layer                      │
│                        (Controller)                          │
├─────────────────────────────────────────────────────────────┤
│                      Business Layer                          │
│                        (Service)                             │
├─────────────────────────────────────────────────────────────┤
│                      Data Access Layer                       │
│                   (Repository / Mapper)                      │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 패키지 구조

```
com.app
├── Application.java              # 애플리케이션 진입점
├── common/                       # 공통 유틸리티
│   ├── ApiResponse.java          # 통일된 API 응답 포맷
│   ├── BusinessException.java    # 비즈니스 예외
│   ├── ErrorCode.java            # 에러 코드 정의
│   ├── GlobalExceptionHandler.java # 전역 예외 처리
│   └── ClientPlatformInterceptor.java # 클라이언트 플랫폼 검증
├── config/                       # 설정 클래스
│   ├── AppProps.java             # 애플리케이션 프로퍼티
│   ├── MySqlDbConfig.java        # MySQL(MyBatis) 설정
│   ├── TimescaleDbConfig.java    # TimescaleDB(MyBatis) 설정
│   ├── OpenApiConfig.java        # Swagger 설정
│   ├── RedisConfig.java          # Redis 설정
│   └── WebMvcConfig.java         # MVC 설정
├── controller/                   # REST API 컨트롤러
│   ├── AuthController.java       # 인증 API
│   ├── MailController.java       # 이메일 인증 API
│   ├── ProposalFileController.java # 제안서 파일 API
│   ├── RtuSimulatorController.java # RTU 시뮬레이터 API
│   ├── S3Controller.java         # 공용 파일 API
│   └── UserController.java       # 사용자 API
├── dto/                          # 데이터 전송 객체
│   ├── LoginRequestDto.java
│   ├── RefreshRequestDto.java
│   ├── TokenResponseDto.java
│   ├── UserSignUpRequestDto.java
│   ├── UserResponse.java
│   ├── ProposalFileRequestDto.java
│   └── ProposalFileResponseDto.java
├── entity/                       # 도메인 엔티티
│   ├── UserEntity.java
│   └── ProposalFileEntity.java
├── mapper/                       # MapStruct 매퍼
│   ├── UserEntityMapper.java
│   └── ProposalFileEntityMapper.java
├── repository/                   # MyBatis 매퍼
│   ├── UserMapper.java
│   └── ProposalFileMapper.java
├── security/                     # 보안 설정
│   ├── SecurityConfig.java       # Spring Security 설정
│   ├── JwtAuthFilter.java        # JWT 인증 필터
│   └── JwtService.java           # JWT 발급/검증
└── service/                      # 비즈니스 로직
    ├── AuthService.java          # 인증 서비스
    ├── MailService.java          # 이메일 서비스
    ├── ProposalFileService.java  # 제안서 파일 서비스
    ├── RtuSimulatorService.java  # RTU 시뮬레이터 서비스
    ├── S3StorageService.java     # S3 스토리지 서비스
    └── UserService.java          # 사용자 서비스
```

---

## 4. 레이어별 책임

### Controller (표현 계층)
- HTTP 요청/응답 매핑
- 요청 파라미터 검증 (`@Valid`)
- DTO를 통한 데이터 교환
- `ResponseEntity<ApiResponse>` 형태로 응답 반환

### Service (비즈니스 계층)
- 비즈니스 로직 구현
- 트랜잭션 경계 관리 (`@Transactional`)
- 여러 Repository/외부 시스템 조합
- 예외 발생 시 `BusinessException` throw

### Repository (데이터 접근 계층)
- MyBatis 매퍼 인터페이스
- SQL 쿼리 실행 (XML 매퍼 파일)
- 비즈니스 로직 포함하지 않음

### Mapper (변환 계층)
- MapStruct 기반 Entity ↔ DTO 변환
- 변환 로직의 일원화

---

## 5. 의존성 규칙

```
Controller → Service → Repository → Entity
     ↓          ↓
    DTO      Mapper
```

**허용되는 방향**:
- `controller` → `service` → `repository` → `entity`
- `controller`, `service` → `dto`, `common`
- `service` → `mapper`

**금지되는 방향**:
- `repository` → `service`
- `service` → `controller`
- `entity` → 상위 레이어

---

## 6. 인증/인가

### JWT 기반 인증
- **Access Token**: 단기 토큰 (기본 5분)
- **Refresh Token**: 장기 토큰 (기본 7일), Redis에 저장

### 토큰 흐름
```
1. 로그인 → Access Token + Refresh Token 발급
2. API 요청 → Authorization: Bearer {accessToken}
3. Access Token 만료 → Refresh Token으로 갱신
4. 로그아웃 → Redis에서 Refresh Token 삭제
```

### Token Rotation
- Refresh Token 사용 시 새로운 Refresh Token 발급
- 이전 토큰은 즉시 무효화
- 탈취 감지 시 Family 전체 무효화

---

## 7. 외부 시스템 연동

### MySQL
- 사용자, 제안서 파일 등 도메인 데이터 저장
- MyBatis를 통한 SQL 매핑

### TimescaleDB
- RTU 시계열(sensor_data) 데이터 저장/조회/집계

### Redis
- Refresh Token 저장 (TTL 7일)
- 이메일 인증 코드 저장 (TTL 설정 가능)
- Key 패턴: `RT:{userId}:{familyId}`, `MAIL:AUTH:{email}`

### AWS S3
- 파일 저장소
- Presigned URL을 통한 임시 접근 제공

---

## 8. API 응답 형식

모든 API는 `ApiResponse` 형식으로 응답합니다.

### 성공 응답
```json
{
  "status": true,
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 에러 응답
```json
{
  "status": false,
  "code": 400,
  "message": "error_message_key",
  "data": null
}
```

---

## 9. 주요 API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/refresh` | 토큰 갱신 | X |
| POST | `/api/auth/logout` | 로그아웃 | O |
| POST | `/api/users/register` | 회원가입 | X |
| POST | `/api/mail/send` | 인증 메일 전송 | X |
| POST | `/api/mail/verify` | 인증 코드 검증 | X |
| POST | `/api/files/upload` | 제안서 파일 업로드 | O |
| GET | `/api/files/list` | 제안서 파일 목록 | O |
| DELETE | `/api/files/{fileId}` | 제안서 파일 삭제 | O |

---

## 10. 설정 파일

### application.yml 주요 설정
```yaml
app:
  cors:
    allowed-origins: http://localhost:3000
  jwt:
    issuer: wattwise
    access-ttl-minutes: 5
    refresh-ttl-days: 7
    secret: ${JWT_SECRET}
  mail:
    from: noreply@example.com
    auth-code-expiration-millis: 300000
  aws:
    s3:
      bucket: ${S3_BUCKET}
      region: ap-northeast-2
```

### 환경 변수 (.env)
```
DB_URL=jdbc:mysql://localhost:3306/wattwise
DB_USERNAME=root
DB_PASSWORD=password
JWT_SECRET=your-secret-key
REDIS_HOST=localhost
REDIS_PORT=6379
S3_BUCKET=wattwise-bucket
S3_ACCESS_KEY=your-access-key
S3_SECRET_KEY=your-secret-key
```

---

## 11. 보안 고려사항

- JWT Secret은 환경 변수로 관리
- 비밀번호는 BCrypt로 해싱
- CORS 화이트리스트 기반 설정
- HttpOnly 쿠키로 토큰 전송 (선택)
- X-Client-Platform 헤더 검증

---

## 12. 개발 가이드

### 새 API 추가 시
1. `dto/` 에 Request/Response DTO 생성
2. `controller/` 에 엔드포인트 추가
3. `service/` 에 비즈니스 로직 구현
4. 필요 시 `repository/` 에 쿼리 추가
5. `SecurityConfig` 에서 권한 설정

### 코드 스타일
- 클래스/인터페이스: PascalCase
- 메서드/변수: camelCase
- 상수: UPPER_SNAKE_CASE
- Lombok 적극 활용
- Javadoc으로 public 메서드 문서화

---

## 13. 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

### Swagger UI
- URL: `http://localhost:8080/swagger-ui.html`
- API 문서 확인 및 테스트 가능

---

## 14. 템플릿 사용 가이드

이 프로젝트를 새 프로젝트의 템플릿으로 사용할 때 참고하세요.

### 필수 수정 항목

1. **패키지명 변경**
   - `com.app` → `com.yourcompany.projectname`
   - 모든 파일의 package 선언 수정

2. **application.yml 설정**
   - `spring.application.name` 변경
   - `app.jwt.issuer` 변경
   - 데이터베이스 URL/계정 설정

3. **환경 변수 (.env)**
   - `JWT_SECRET`: 새로운 시크릿 키 생성
   - DB, Redis, S3 접속 정보 설정

4. **OpenApiConfig.java**
   - API 문서 제목/설명 수정

### 선택적 수정 항목

1. **도메인별 패키지 분리** (규모가 커질 경우)
   ```
   com.app.user.controller
   com.app.user.service
   com.app.proposal.controller
   com.app.proposal.service
   ```

2. **프로파일 분리** (운영 환경)
   - `application-local.yml`
   - `application-prod.yml`
   - Swagger 접근 제한 (운영 환경)

3. **테스트 코드 추가**
   - `src/test/java/com/app/service/*Test.java`

### 제거 가능한 도메인

 프로젝트에 맞게 불필요한 도메인을 제거하세요:

- **RTU 시뮬레이터**: `RtuSimulatorController`, `RtuSimulatorService`, `TimescaleDbConfig`
 - **제안서 파일**: `ProposalFile*` 관련 클래스들
 - **공용 파일**: `S3Controller` (ProposalFileController만 사용 시)

### 보안 체크리스트

- [ ] JWT Secret 변경 (최소 256bit)
- [ ] 운영 환경에서 Swagger 비활성화
- [ ] CORS 허용 도메인 제한
- [ ] H2 콘솔 비활성화 (운영)
- [ ] 로그에 민감 정보 노출 확인
