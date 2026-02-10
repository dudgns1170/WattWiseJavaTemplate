---
trigger: always_on
---

---
trigger: always_on
description: WattWise Java Backend import 스타일 가이드
globs: "src/main/java/**/*.java"
---

- **[그룹 순서]**
  1. `java.*`, `javax.*` / JDK 표준 라이브러리
  2. `org.springframework.*`, `jakarta.*`, `io.jsonwebtoken.*` 등 외부 라이브러리
  3. `com.app.*` 애플리케이션 내부 코드
  4. `static` import (필요한 경우에만)

- **[그룹 간 공백]**
  - 그룹 사이에는 한 줄 공백을 둔다.
  - 같은 그룹 안에서는 알파벳 순으로 정렬한다.

- **[와일드카드 금지]**
  - `import com.app.service.*;` 와 같은 `*` import는 사용하지 않는다.
  - IDE 설정에서 자동 와일드카드 사용을 비활성화한다.

- **[사용하지 않는 import]**
  - 사용되지 않는 import는 즉시 제거한다.  
    (IDE의 optimize imports 기능을 적극 사용)

- **[명확한 출처]**
  - 같은 이름의 타입이 여러 패키지에 있을 경우  
    (예: `List`, `Date` 등)  
    어떤 패키지에서 오는지 코드 리뷰 시 항상 의식적으로 확인한다.