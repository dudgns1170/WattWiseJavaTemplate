# Java Controller / Service 기본 패턴 (단건 vs 리스트)

WattWise Java 백엔드에서 **Controller / Service / Repository** 계층에서
"단건"과 "리스트"를 어떻게 나눠서 사용할지 정리한 가이드입니다.

---

## 1. 기본 원칙

- **Controller**
  - HTTP 요청/응답 담당.
  - 항상 DTO를 받고 DTO를 반환한다.
  - 단건이면 `ResponseEntity<SomethingResponseDto>`.
  - 리스트면 `ResponseEntity<List<SomethingResponseDto>>`.
- **Service**
  - 비즈니스 로직 담당.
  - Controller와 동일하게 **단건이면 DTO, 여러 건이면 List<DTO>** 를 반환.
  - 입력도 요구사항에 따라 `SomethingRequestDto` 또는 `List<SomethingRequestDto>` 사용.
- **Repository (Mapper)**
  - MyBatis 매퍼.
  - 단건이면 `SomethingEntity`.
  - 리스트면 `List<SomethingEntity>`.

---

## 2. "단건" vs "리스트" 기준

- **단건 (One)**
  - ID로 하나 조회할 때: `userId`, `proposalNo` 등.
  - 결과가 0개 또는 1개인 것이 비즈니스적으로 자연스러운 경우.
- **리스트 (Many)**
  - 검색 조건으로 여러 개 조회할 때.
  - 화면에 테이블/리스트로 뿌릴 데이터를 가져올 때.
  - 클라이언트에서 여러 개를 한 번에 등록/수정/삭제할 때.

---

## 3. Controller: 요청/응답 타입 패턴

### 3.1. 조회 (GET)

- **단건 조회**
  - 경로: `/users/{userId}`
  - 메서드 반환 타입: `ResponseEntity<UserResponseDto>`
  - Service 호출: `UserResponseDto getUser(Long userId)`

- **리스트 조회**
  - 경로: `/users`
  - 메서드 반환 타입: `ResponseEntity<List<UserResponseDto>>`
  - Service 호출: `List<UserResponseDto> getUsers(UserSearchRequestDto requestDto)`

> 정리: **단건이면 DTO, 리스트면 List<DTO>를 ResponseEntity에 싸서 리턴**.

### 3.2. 생성 (POST)

- **단건 생성**
  - 요청 바디: `UserCreateRequestDto`
  - 반환 타입(권장): `ResponseEntity<UserResponseDto>` 혹은 `ResponseEntity<Void>`
  - Service 메서드: `UserResponseDto createUser(UserCreateRequestDto requestDto)`

- **여러 개 한 번에 생성 (배치)**
  - 요청 바디: `List<UserCreateRequestDto>`
  - 반환 타입: `ResponseEntity<List<UserResponseDto>>` 또는 `ResponseEntity<Void>`
  - Service 메서드: `List<UserResponseDto> createUsers(List<UserCreateRequestDto> requestDtos)`

> 입력 JSON이 배열이면 `List<...>` 로 받고,
> JSON 객체이면 단건 `...Dto` 로 받는다고 기억하면 된다.

### 3.3. 수정 (PUT / PATCH)

- **단건 수정**
  - 경로: `/users/{userId}`
  - 요청 바디: `UserUpdateRequestDto`
  - 반환 타입: `ResponseEntity<UserResponseDto>` 또는 `ResponseEntity<Void>`
  - Service 메서드: `UserResponseDto updateUser(Long userId, UserUpdateRequestDto requestDto)`

- **여러 개 수정**
  - 경로: `/users`
  - 요청 바디: `List<UserUpdateRequestDto>`
  - 반환 타입: `ResponseEntity<List<UserResponseDto>>` 또는 `ResponseEntity<Void>`
  - Service 메서드: `List<UserResponseDto> updateUsers(List<UserUpdateRequestDto> requestDtos)`

### 3.4. 삭제 (DELETE)

- **단건 삭제**
  - 경로: `/users/{userId}`
  - 반환 타입: `ResponseEntity<Void>`
  - Service 메서드: `void deleteUser(Long userId)`

- **여러 개 삭제**
  - 경로 예시: `/users`
  - 요청 바디: `List<Long>` (userId 리스트)
  - 반환 타입: `ResponseEntity<Void>`
  - Service 메서드: `void deleteUsers(List<Long> userIds)`

---

## 4. Service 계층에서의 단건/리스트 규칙

### 4.1. 단건 규칙

- 메서드명: `getUser`, `findUser`, `createUser`, `updateUser`, `deleteUser` 등 **단수형**.
- 반환 타입:
  - 조회: `UserResponseDto` 또는 `Optional<UserResponseDto>`
  - 생성/수정: `UserResponseDto` (생성/수정 결과를 바로 리턴하고 싶을 때)
  - 삭제: `void`
- Repository 호출:
  - `UserEntity findUserById(Long userId)`

### 4.2. 리스트 규칙

- 메서드명: `getUsers`, `findUsers`, `createUsers`, `updateUsers`, `deleteUsers` 등 **복수형**.
- 반환 타입:
  - 조회: `List<UserResponseDto>`
  - 생성/수정: `List<UserResponseDto>` (필요시)
  - 삭제: `void`
- Repository 호출:
  - `List<UserEntity> findUsers(UserSearchCondition condition)`

> 기억 포인트: **Service도 Controller와 동일하게 단건이면 DTO, 여러 건이면 List<DTO>를 반환**.

---

## 5. Repository (Mapper)에서의 단건/리스트

- **단건 조회**
  - 메서드명 예시: `findUserById`
  - 반환 타입: `UserEntity` 또는 `UserEntity?` (MyBatis는 보통 null 가능)
- **리스트 조회**
  - 메서드명 예시: `findUsers`
  - 반환 타입: `List<UserEntity>`

- **단건 INSERT/UPDATE/DELETE**
  - 반환 타입: `int` (영향을 받은 row 수) 또는 `void` (관례에 따라).
- **배치 INSERT/UPDATE/DELETE**
  - 파라미터 타입: `List<UserEntity>` 또는 `List<Long>` 등.
  - 반환 타입: `int` 또는 `void`.

> Repository는 항상 **Entity 또는 List<Entity>** 기준으로 생각하고,
> 위/아래 레이어에서 DTO로 변환하는 구조를 유지.

---

## 6. JSON 구조와 타입 매핑 기억법

- **클라이언트 JSON이 객체**
  - 예: `{ "name": "test", "age": 20 }`
  - Controller 파라미터: `UserCreateRequestDto requestDto`
- **클라이언트 JSON이 배열**
  - 예: `[ { "name": "a" }, { "name": "b" } ]`
  - Controller 파라미터: `List<UserCreateRequestDto> requestDtos`

- **응답도 동일**
  - 단건 응답 JSON: `{ ... }` → `UserResponseDto`
  - 리스트 응답 JSON: `[ { ... }, { ... } ]` → `List<UserResponseDto>`

---

## 7. 네이밍 작은 규칙 정리

- 단건 메서드 이름: `getUser`, `findUser`, `createUser`, `updateUser`, `deleteUser`.
- 리스트 메서드 이름: `getUsers`, `findUsers`, `createUsers`, `updateUsers`, `deleteUsers`.
- DTO 이름:
  - 요청: `UserCreateRequestDto`, `UserUpdateRequestDto`, `UserSearchRequestDto`.
  - 응답: `UserResponseDto`, `UserListItemResponseDto` 등.

---

## 8. 빠르게 떠올리기 위한 요약

- **Controller**
  - 단건: `ResponseEntity<SomethingResponseDto>`
  - 여러 건: `ResponseEntity<List<SomethingResponseDto>>`
  - 요청 바디가 배열이면 `List<...RequestDto>`.
- **Service**
  - 단건: `SomethingResponseDto` / `Optional<SomethingResponseDto>`
  - 여러 건: `List<SomethingResponseDto>`
- **Repository**
  - 단건: `SomethingEntity`
  - 여러 건: `List<SomethingEntity>`

앞으로 "단건 vs 리스트"가 헷갈리면 **1) JSON 구조(객체/배열)**를 먼저 떠올리고,
그 다음에 **DTO vs List<DTO>** 로 매핑한다는 것만 기억하면 된다.
