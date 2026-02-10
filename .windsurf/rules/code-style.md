---
trigger: always_on
---

---
trigger: always_on
description: WattWise Java Backend Code Style
globs: "src/main/java/**/*.java", "src/test/java/**/*.java"
---ㅇ

# 1. General (공통 원칙)

- **[언어]**  
  - 모든 클래스/메서드/변수 이름은 **영어**로 작성한다.  
  - Javadoc와 설명 주석은 한국어 사용을 허용한다.
- **[파일 단위 책임]**  
  - 파일 하나당 하나의 `public` 클래스를 원칙으로 한다.  
  - 한 클래스는 한 가지 도메인 책임에 집중한다.
- **[Lombok 활용]**  
  - DTO, 단순 데이터 홀더, 엔티티에는 Lombok(`@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@RequiredArgsConstructor`)을 적극 사용해 보일러플레이트 코드를 줄인다.
- **[Javadoc]**  
  - `controller`, `service`의 public 메서드는 가능한 Javadoc으로  
    “역할, 요청/응답 예시, 예외 상황”을 짧게라도 명시한다.  
  - [BACKEND_API_AND_STRUCTURE.md](cci:7://file:///d:/WattWiseJava/BACKEND_API_AND_STRUCTURE.md:0:0-0:0)의 설명과 최대한 일관되게 유지한다.

---

# 2. Packages & Layers (패키지 / 레이어)

- **[루트 패키지]**  
  - 루트 패키지는 `com.app`을 사용한다.  
  - 도메인/레이어 구조는 다음을 기본으로 한다.  
    - `com.app.config`, `com.app.security`, `com.app.controller`, `com.app.service`,  
      `com.app.repository`, `com.app.mapper`, `com.app.entity`, `com.app.dto`, `com.app.common`
- **[레이어 간 의존성]**  
  - `controller` → `service` → `repository`/`mapper` → `entity` 방향만 허용한다.
  - `controller`는 `dto`, `common`에 의존할 수 있다.
  - `service`는 `dto`, `mapper`, `entity`, `repository`, `common`에 의존할 수 있다.
  - `repository`/`mapper`는 `entity`, `common`에만 의존한다.
  - `entity`는 어떤 상위 레이어(`controller`, `service`, `repository`, `security`)에도 의존하지 않는다.
- **[도메인 기준 구조]**  
  - 규모가 커질 경우, 도메인 기준 하위 패키지를 사용한다.  
    - 예: `com.app.user.controller`, `com.app.user.service`, `com.app.user.dto`  
    - 예: `com.app.proposal.controller`, `com.app.proposal.service`, `com.app.proposal.entity`

---

# 3. Naming (네이밍 규칙)

- **[클래스/인터페이스]**  
  - PascalCase: `AuthController`, `UserService`, `ProposalFileEntity`, `TokenResponseDto`.
- **[메서드/변수]**  
  - camelCase: `login`, `refresh`, `fileUpload`, `currentUserId`, `proposalNo`.
- **[상수]**  
  - UPPER_SNAKE_CASE: `JWT_SECRET_KEY`, `ACCESS_TOKEN_COOKIE_NAME`.
- **[DTO 네이밍]**  
  - 요청: `LoginRequestDto`, `UserSignUpRequestDto`, `ProposalFileRequestDto`.  
  - 응답: `TokenResponseDto`, `UserResponse`, `ProposalFileResponseDto`.
- **[패키지 이름]**  
  - 모두 소문자, 약어를 과도하게 쓰지 않는다.  
  - 도메인 + 레이어를 명확히 드러낸다. (예: `proposal`, `auth`, `user` 등)

---

# 4. Classes & Methods (클래스/메서드 스타일)

- **[메서드 책임/길이]**  
  - 메서드는 **한 가지 유스케이스/목적**만 수행하도록 작성한다.  
  - 복잡해질 경우 private 메서드로 분리하여 20줄 안팎을 목표로 한다.
- **[중첩 최소화]**  
  - if/else 중첩이 깊어지면 **early return**이나 메서드 분리로 가독성을 높인다.
- **[Controller 규칙]**  
  - `@RestController` + `@RequestMapping`을 사용하여 API를 정의한다.  
  - 메서드는 `@GetMapping`, `@PostMapping`, `@DeleteMapping` 등 HTTP 메서드에 맞는 어노테이션을 사용한다.  
  - 요청/응답은 `RequestDto`, `ResponseDto` + `ResponseEntity<T>`를 사용하고,  
    엔티티를 직접 반환하지 않는다.
- **[Service 규칙]**  
  - `@Service`와 `@RequiredArgsConstructor`를 사용해 의존성 주입을 구성한다.  
  - 비즈니스 로직, 트랜잭션 경계(`@Transactional`)를 Service에 둔다.  
  - Controller에서 넘어온 DTO를 사용해 유즈케이스를 구현하고,  
    필요한 경우 `mapper`/엔티티를 사용해 DB 연동을 수행한다.
- **[Stream / 컬렉션 처리]**  
  - 간단한 필터링/매핑/수집은 Java Stream API를 활용하되,  
    과도한 한 줄짜리 체이닝보다 읽기 쉬운 수준을 우선한다.

---

# 5. DTO / Entity / Mapper

- **[DTO]**  
  - `com.app.dto`에 위치하며, 요청/응답 전용으로 사용한다.  
  - Lombok을 사용해 게터/세터/생성자/빌더를 정의한다.  
  - 비즈니스 로직은 넣지 않고, 필요한 경우에만 정적 팩터리(`from(...)`) 정도만 허용한다.
- **[Entity]**  
  - `com.app.entity`에 위치한다.  
  - DB 스키마와 직접 매핑되는 필드명을 사용하되,  
    DTO에서는 도메인 친화적인 이름(`proposalNo`, `fileUrl` 등)을 사용한다.  
  - Web/HTTP 관련 어노테이션이나 의존성을 갖지 않는다.
- **[Mapper]**  
  - `com.app.mapper`에 MapStruct 기반 매퍼 인터페이스를 정의한다.  
  - Entity ↔ DTO 변환은 가급적 매퍼에서 처리하고,  
    Service/Controller 내에 변환 로직이 흩어지지 않도록 한다.

---

# 6. Null, Optional, Collections (null, Optional, 컬렉션)

- **[null 처리]**  
  - API 입력 값 검증은 DTO + Bean Validation(`@Valid`, `@NotNull` 등)으로 우선 처리하고,  
    Service 메서드는 “검증된 값”을 전제로 구현한다.  
  - 가능하면 null 대신 빈 컬렉션/옵션 값을 사용하는 것을 우선 고려한다.
- **[Optional 사용]**  
  - 반환 타입에서 “값이 없을 수 있음”을 표현할 때 `Optional<T>`를 사용한다.  
  - 필드(엔티티/DTO)에는 `Optional`을 사용하지 않는다.
- **[불변성]**  
  - 변경되지 않는 필드는 `final`을 사용해 의도를 명확히 한다.  
  - 외부에서 받은 컬렉션은 가능한 복사하거나 수정 불가 컬렉션으로 다룬다.

---

# 7. Exception & Error Handling (예외/에러 처리)

- **[예외 종류]**  
  - 비즈니스 규칙 위반은 도메인 의미가 드러나는 예외(또는 공통 `BusinessException`)로 표현하는 것을 권장한다.  
  - HTTP 상태 코드를 직접 제어해야 할 때는 `ResponseStatusException` 또는  
    `@RestControllerAdvice` + 공통 에러 응답 포맷 사용을 우선 고려한다.
- **[예외 처리 위치]**  
  - Service에서 의미 있는 예외를 던지고,  
  - Controller 혹은 Global Exception Handler에서 HTTP 응답으로 변환한다.
- **[메시지]**  
  - 예외 메시지는 사용자/개발자가 이해할 수 있도록  
    “무슨 상황에서 왜 실패했는지”를 한 문장으로 명확히 표현한다.
- **[RuntimeException 사용]**  
  - 단순 `new RuntimeException(...)` 사용은 임시 디버깅 단계에서만 허용하고,  
    실제 로직에서는 의미 있는 커스텀 예외 또는 `ResponseStatusException`으로 정리해 나간다.

---

# 8. Logging (로깅)

- **[Logger 사용]**  
  - `System.out.println`은 디버깅 상황에서만 임시로 사용하고,  
    커밋 전에는 제거하거나 로거로 교체한다.  
  - SLF4J 기반 로거(`log.info`, `log.error`, `log.debug`)를 사용한다.
- **[로그 레벨]**  
  - 정상 흐름 요약: `INFO`  
  - 상세 디버깅용 정보: `DEBUG`  
  - 예외/오류: `ERROR`
- **[민감 정보]**  
  - 비밀번호, 토큰, JWT, Secret Key, 개인 정보 등은  
    로그에 남기지 않는다.

---

# 9. Test Code (테스트 코드)

- **[위치]**  
  - 테스트 코드는 `src/test/java/com/app/...` 아래에 위치시킨다.
- **[네이밍]**  
  - 테스트 클래스는 대상 클래스명 + `Test`를 붙인다.  
    - 예: `AuthServiceTest`, `ProposalFileServiceTest`.  
  - 테스트 메서드는 “상황 + 기대 결과”가 드러나도록 작성한다.  
    - 예: `login_ShouldThrowException_WhenPasswordDoesNotMatch`.
- **[레벨]**  
  - Service 레벨에서 비즈니스 규칙(회원가입, 파일 업로드/삭제, 토큰 발급/갱신 등)에 대한 테스트를 우선 작성한다.  
  - Controller 테스트는 주요 API 흐름이 깨지지 않았는지 확인하는 용도로 사용한다.