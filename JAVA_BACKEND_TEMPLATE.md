JAVA_BACKEND_TEMPLATE

# WattWise Java Backend Template

## 개요

- Docker, Swagger(Springdoc OpenAPI 3)를 포함한 재사용 가능한 Spring Boot REST 백엔드 템플릿
- `.env`와 `application.yml` 기반 환경 변수 구성, MySQL, Redis, AWS S3, Mail, JWT 설정 분리
- 인증/회원, 메일 인증, 공용/제안서 파일 업로드, RTU 시뮬레이터 등 도메인 API 제공
- 한국어 도큐멘테이션과 Swagger 기반 자동 스키마/문서 엔드포인트 제공

## 주요 스택

### Backend
- **Language**: Java 17+
- **Framework**: Spring Boot 3.x
- **ORM/Persistence**: MyBatis (MySQL)
- **Database**: MySQL 8.0
- **Cache**: Redis
- **Security**: Spring Security + JWT (jjwt)
- **API Documentation**: Springdoc OpenAPI 3 (Swagger UI)
- **Build Tool**: Gradle 8.x (Gradle Wrapper)

### Infrastructure
- **Cloud**: AWS (EC2, RDS, S3, ALB)
- **Server**: Ubuntu/Amazon Linux 2
- **Reverse Proxy**: Nginx (optional)
- **SSL**: AWS ACM (Application Load Balancer)
- **Domain**: Route 53

## 디렉터리 개요

```text
WattWiseJava/
├─ build.gradle              # Spring Boot 및 의존성 설정
├─ .env                      # 운영/개발 공용 환경 변수 (메일, DB, AWS 등)
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/app/
│  │  │     ├─ Application.java      # Spring Boot 엔트리포인트
│  │  │     ├─ config/               # 설정 (AppProps, CORS, Security 등)
│  │  │     ├─ controller/           # REST API 컨트롤러
│  │  │     ├─ dto/                  # 요청/응답 DTO
│  │  │     ├─ entity/               # 엔티티(도메인 객체)
│  │  │     ├─ mapper/               # MyBatis 매퍼 인터페이스
│  │  │     ├─ repository/           # Repository, DAO 레이어
│  │  │     ├─ security/             # 인증/인가 (JWT, 필터, SecurityConfig 등)
│  │  │     └─ service/              # 비즈니스 로직
│  │  ├─ resources/
│  │  │  ├─ application.yml          # db, redis, aws, mail 등 설정
│  │  │  ├─ mapper/                  # MyBatis XML 매퍼
│  │  │  └─ schema.sql               # 초기 스키마 정의
│  └─ test/                          # 단위/통합 테스트
└─ BACKEND_API_AND_STRUCTURE.md      # 구조 및 API 명세
```

## 아키텍처

### 시스템 아키텍처
```
[Client] → [Route 53] → [ALB + SSL] → [EC2: Spring Boot App] → [RDS MySQL]
                                              ↓
                                          [Redis Cache]
                                              ↓
                                          [AWS S3]
```

### 애플리케이션 레이어 구조
```
Controller Layer (REST API)
    ↓
Service Layer (Business Logic)
    ↓
Repository/Mapper Layer (Data Access)
    ↓
Database (MySQL)
```

## 빠른 시작

### 1. 사전 요구사항
- Java 17 이상
- Gradle 8.x 이상 (Gradle Wrapper 포함)
- MySQL 8.0
- Redis (선택사항)

### 2. 환경 설정
`.env` 파일 생성 (루트 디렉토리):
```properties
# ===================== JWT / 보안 =====================
JWT_SECRET=your_jwt_secret
JWT_ACCESS_TTL_MINUTES=30
JWT_REFRESH_TTL_DAYS=7
JWT_ISSUER=http://localhost:8080

# ===================== Database =====================
DB_URL=jdbc:mysql://localhost/wattwise_dev?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=root
DB_PASSWORD=your_db_password

# ===================== Mail (Gmail) =====================
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_app_password

# ===================== Redis =====================
REDIS_HOST=localhost
REDIS_PORT=6379

# ===================== AWS S3 =====================
AWS_S3_BUCKET=your_bucket_name
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_access_key_id
AWS_SECRET_ACCESS_KEY=your_secret_access_key
AWS_S3_BASE_URL=https://your-bucket.s3.ap-northeast-2.amazonaws.com

# ===================== CORS =====================
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:5173

# ===================== 외부 인증 (선택) =====================
CLERK_SECRET_KEY=your_clerk_secret_key
```

### 3. 로컬 실행
```bash
# 1. 프로젝트 클론
git clone https://github.com/your-org/WattWiseJava.git
cd WattWiseJava

# 2. 의존성 설치 및 빌드
./gradlew clean build

# 3. 애플리케이션 실행
./gradlew bootRun

# 또는 JAR 파일 실행
java -jar build/libs/backend-0.0.1-SNAPSHOT.jar
```

### 4. Docker 실행 (선택사항)
```bash
# Docker 이미지 빌드
docker build -t wattwise-backend .

# 컨테이너 실행
docker run -p 8080:8080 --env-file .env wattwise-backend
```

### 5. 접속 확인
- API 서버: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health Check: `http://localhost:8080/actuator/health`

## 환경 변수

- `.env` 파일을 루트 디렉토리에 생성하고 `application.yml`에서 `${...}` 형태로 참조합니다.
- 주요 키:
  - `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET`, `JWT_ACCESS_TTL_MINUTES`, `JWT_REFRESH_TTL_DAYS`, `JWT_ISSUER`
  - `MAIL_USERNAME`, `MAIL_PASSWORD`
  - `REDIS_HOST`, `REDIS_PORT`
  - `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BASE_URL`
  - `CORS_ALLOWED_ORIGINS`, `CLERK_SECRET_KEY`
- 상세한 예시는 위 `빠른 시작 > 2. 환경 설정` 섹션의 `.env` 예시를 참고합니다.

## 명령어

- `./gradlew clean build` : 전체 빌드
- `./gradlew bootRun` : 로컬 개발 서버 실행
- `./gradlew test` : 테스트 실행

## 트러블슈팅

- **DB 연결 오류**  
  - `DB_URL`, 포트, 방화벽 설정 및 MySQL 계정 정보를 확인합니다.
- **JWT 401/403 오류**  
  - 프론트엔드와 백엔드가 동일한 `JWT_SECRET`, TTL 설정을 사용하는지 확인합니다.
- **AWS S3 업로드/다운로드 실패**  
  - `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_S3_BASE_URL` 값을 검증하고 IAM 권한을 확인합니다.

## Swagger/스키마 확장 팁

- `org.springdoc:springdoc-openapi-starter-webmvc-ui` 의존성을 사용하여 Swagger UI를 제공합니다.
- 각 컨트롤러에 `@Tag`, 메서드에 `@Operation`을 부여하고, 요청/응답 DTO에 `@Schema`를 붙여 필드 설명을 추가합니다.
- 이 문서 및 `BACKEND_API_AND_STRUCTURE.md`의 예시 JSON 구조를 Swagger 스키마 example로 복사해 사용할 수 있습니다.

## 기타 참고 문서
- **상세 백엔드 구조 및 API 명세**: `BACKEND_API_AND_STRUCTURE.md` 문서를 참고합니다.