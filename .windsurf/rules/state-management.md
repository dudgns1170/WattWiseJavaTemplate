---
trigger: always_on
---

---
trigger: always_on
description: WattWise Java Backend state-management 규칙
globs: "src/main/java/**/*.java"
---

- **[Stateless 서비스]**
  - `@Service` 빈은 가능한 한 **stateless**하게 유지한다.
  - 요청 간 공유되는 mutable 필드를 두지 않는다.
  - 상태는 DB, Redis, 외부 시스템(MySQL, S3 등)에 저장하고,  
    서비스 인스턴스에는 담지 않는다.

- **[트랜잭션 경계]**
  - 비즈니스 트랜잭션 경계는 Service 계층에서 `@Transactional`로 관리한다.
  - 쓰기 작업(등록/수정/삭제)은 기본 트랜잭션,  
    조회 전용은 `@Transactional(readOnly = true)`를 사용해 의도를 드러낸다.

- **[인증/세션 상태]**
  - 인증 상태는 Spring Security + JWT로 관리한다.
  - Access Token은 stateless로 처리하고,  
    Refresh Token/세션 관련 상태는 Redis 등 외부 스토어에 저장한다.
  - `SecurityContextHolder`를 통해 필요한 최소한의 인증 정보만 조회하고,  
    컨트롤러/서비스에 불필요하게 세션 개념을 흩뿌리지 않는다.

- **[캐시/Redis]**
  - Redis는 토큰, 인증 코드, 임시 데이터 등 **명확한 만료 기준**이 있는 데이터에 사용한다.
  - Redis 키 네이밍은 접두어로 도메인을 구분한다.  
    - 예: `RT:{userId}:{familyId}`, `MAIL_CODE:{email}`.
  - 캐시/Redis 접근은 Service 또는 별도 infra/service 계층에 모으고,  
    Controller에서 직접 다루지 않는다.

- **[전역/정적 상태]**
  - 전역 `static` mutable 상태(전역 리스트, 맵 등)는 두지 않는다.
  - 반드시 필요하면, 설정/상수 또는 Bean으로 관리하고,  
    가변 데이터는 항상 외부 스토어(DB/Redis)에 둔다.

- **[동시성 고려]**
  - 동일 자원에 대한 갱신 로직(예: 토큰 로테이션, 파일 메타데이터 수정)은  
    **트랜잭션 + 저장소 레벨**에서 일관성이 유지되도록 설계한다.
  - 여러 노드(멀티 인스턴스) 환경에서도 문제가 없는지 항상 전제하고 설계한다.