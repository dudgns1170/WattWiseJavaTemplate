# WattWise Backend

Spring Boot 3.x + MyBatis 기반의 3-Layered Architecture 백엔드 프로젝트 템플릿

## 🚀 프로젝트 설정

### 저장소 복제

```bash
git clone https://github.com/Solarteq/WattWiseJava.git
cd WattWiseJava
```

### 환경 설정

1. `.env.example`을 복사하여 `.env` 생성
2. 필요한 환경변수 설정

```bash
# .env 예시
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

### 빌드 및 실행

```bash
# 빌드
./gradlew build

# 실행
./gradlew bootRun

# 테스트
./gradlew test
```

## 🛠 기술 스택

| 분류 | 기술 |
|------|------|
| **Framework** | Spring Boot 3.2.x |
| **Language** | Java 17 |
| **Build Tool** | Gradle |
| **Database** | MySQL + MyBatis |
| **Cache** | Redis |
| **Security** | Spring Security + JWT |
| **File Storage** | AWS S3 |
| **API Docs** | Swagger (OpenAPI 3.0) |
| **Mapping** | MapStruct |
| **Utility** | Lombok |

## 📁 아키텍처

이 프로젝트는 **3-Layered Architecture**를 따릅니다.

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

### 패키지 구조

```
com.app
├── Application.java              # 애플리케이션 진입점
├── common/                       # 공통 유틸리티, 예외 처리
├── config/                       # 설정 클래스
├── controller/                   # REST API 컨트롤러
├── dto/                          # 데이터 전송 객체
├── entity/                       # 도메인 엔티티
├── mapper/                       # MapStruct 매퍼
├── repository/                   # MyBatis 매퍼
├── security/                     # 보안 설정, JWT
└── service/                      # 비즈니스 로직
```

## 🔐 인증/인가

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

## 📡 주요 API 엔드포인트

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|:----:|
| POST | `/api/auth/login` | 로그인 | ✗ |
| POST | `/api/auth/refresh` | 토큰 갱신 | ✗ |
| POST | `/api/auth/logout` | 로그아웃 | ✓ |
| POST | `/api/users/register` | 회원가입 | ✗ |
| POST | `/api/mail/send` | 인증 메일 전송 | ✗ |
| POST | `/api/mail/verify` | 인증 코드 검증 | ✗ |
| POST | `/api/files/upload` | 파일 업로드 | ✓ |
| GET | `/api/files/list` | 파일 목록 | ✓ |
| DELETE | `/api/files/{fileId}` | 파일 삭제 | ✓ |

### Swagger UI

- URL: `http://localhost:8080/swagger-ui.html`

## 외부 시스템 연동

- **MySQL**: 도메인 데이터 저장 (MyBatis)
- **Redis**: Refresh Token, 이메일 인증 코드 저장
- **AWS S3**: 파일 저장소, Presigned URL 제공
- **TimescaleDB**: RTU 시계열(sensor_data) 데이터 저장/조회/집계

## 템플릿 사용 가이드

### 필수 수정 항목

1. **패키지명 변경**: `com.app` → `com.yourcompany.projectname`
2. **application.yml**: `spring.application.name`, `app.jwt.issuer` 변경
3. **환경 변수 (.env)**: JWT_SECRET, DB, Redis, S3 접속 정보 설정
4. **OpenApiConfig.java**: API 문서 제목/설명 수정


