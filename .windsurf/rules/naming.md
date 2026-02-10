---
trigger: always_on
---

---
trigger: always_on
description: WattWise Java Backend naming 규칙
globs: "src/main/java/**/*.java"
---

- **[클래스/인터페이스]**
  - PascalCase 사용: `AuthController`, `UserService`, `ProposalFileEntity`, `JwtService`.
  - 도메인 + 역할이 드러나도록 이름짓는다.  
    - 예: `ProposalFileService`, `MailController`, `RtuSimulatorController`.

- **[메서드/변수]**
  - camelCase 사용: `login`, `refresh`, `fileUpload`, `proposalNo`, `userId`.
  - 메서드 이름은 “무엇을 하는지” 한 눈에 보이도록 작성한다.  
    - 예: `fileUpload`, `listUsers`, `resolveCurrentUserId`.

- **[상수]**
  - UPPER_SNAKE_CASE 사용: `JWT_SECRET_KEY`, `ACCESS_TOKEN_TTL_MINUTES`.
  - 의미가 분명한 이름을 사용하고, “매직 넘버/문자열”은 상수로 뽑는다.

- **[DTO 네이밍]**
  - Request: `LoginRequestDto`, `UserSignUpRequestDto`, `ProposalFileRequestDto`.
  - Response: `TokenResponseDto`, `UserResponse`, `ProposalFileResponseDto`.
  - 이름만 보고 “클라이언트 → 서버”인지, “서버 → 클라이언트”인지 알 수 있게 한다.

- **[패키지/도메인]**
  - 패키지는 모두 소문자: `com.app.auth`, `com.app.user`, `com.app.proposal`.
  - 도메인 + 레이어를 조합해 구조를 잡는다.  
    - 예: `com.app.proposal.controller`, `com.app.proposal.service`, `com.app.proposal.dto`.

- **[테스트]**
  - 테스트 클래스: 대상 클래스명 + `Test` → `AuthServiceTest`, `ProposalFileServiceTest`.
  - 테스트 메서드: `methodName_조건_기대결과` 패턴 권장.  
    - 예: `login_ShouldThrowException_WhenPasswordDoesNotMatch`.