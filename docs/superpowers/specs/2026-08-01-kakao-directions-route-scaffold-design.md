# 카카오모빌리티 길찾기(Directions) 연동 스캐폴딩 설계

- 날짜: 2026-08-01
- 상태: 승인됨

## 배경

추천 결과로 5~6개의 관광지가 순서대로 제시되면, 추천 순서상 인접한 구간(예: 1→2, 2→3, ...)의
이동시간을 카카오모빌리티 길찾기 API(`GET /v1/future/directions`)로 조회해 보여줄 예정이다.
추천 로직 자체는 아직 구현되어 있지 않으므로, 이번 작업은 추천 도메인과 독립적으로 재사용 가능한
"좌표 리스트 → 인접 구간 이동시간 리스트" 스캐폴딩만 만든다.

N개 좌표가 주어지면 인접 구간은 N-1개이며, 5~6개 관광지 기준 최대 5회의 외부 API 호출이 발생한다.

## 아키텍처

새 도메인 `com.sodosiro.domain.route`를 만든다. 카카오모빌리티 API는 이 도메인 내부의 얇은 클라이언트로
감싸고, `RestClient` 빈은 기존 `RestTemplateConfig`와 같은 위치(`global/config`)에 둔다. 도메인 서비스는
순서가 있는 좌표 리스트를 받아 인접 구간을 가상 스레드로 병렬 호출하고 결과를 원래 순서대로 취합한다.

추천 도메인이 아직 없으므로, 이 스캐폴딩은 `List<RouteWaypoint>`(좌표 DTO)를 입력으로 받는
독립적인 컴포넌트로 만든다. 추천 서비스가 생기면 `TouristSpot → RouteWaypoint` 변환만 추가하면 된다.

## 컴포넌트

- `KakaoMobilityRestClientConfig` (`global/config`)
  - `RestClient` 빈 1개. baseUrl `https://apis-navi.kakaomobility.com`, 기본 헤더
    `Authorization: KakaoAK {REST API 키}` 고정. connect/read 타임아웃 5초(기존 `RestTemplateConfig`와 동일).
- `RouteWaypoint` (`domain/route/dto`)
  - `record RouteWaypoint(Long id, BigDecimal x, BigDecimal y)` — 좌표 DTO. `TouristSpot`에 의존하지 않는다.
- `KakaoDirectionsClient` (`domain/route/client`)
  - 단건 호출만 담당: `DirectionsLegResult findRoute(RouteWaypoint origin, RouteWaypoint destination, LocalDateTime departureTime)`
  - 카카오 응답에서 필요한 필드(`duration`, `distance`, `result_code`)만 매핑한다. 응답 전체를 매핑하지 않는다.
- `RouteLegTimeService` (`domain/route/service`)
  - `List<RouteLeg> calculateAdjacentLegTimes(List<RouteWaypoint> orderedWaypoints, LocalDateTime departureTime)`
  - 인접 쌍 (0,1),(1,2),...,(n-2,n-1)을 만들어 가상 스레드로 병렬 호출 후 원래 순서대로 결과를 반환한다.
- `RouteLeg` (`domain/route/dto`)
  - `record RouteLeg(Long fromId, Long toId, Long durationSeconds, Long distanceMeters, boolean success)`
  - 실패한 구간은 `success=false`, `durationSeconds`/`distanceMeters`는 `null`.

## 데이터 흐름

1. (미래) 추천 서비스가 추천된 스팟을 순서대로 `RouteWaypoint` 리스트로 변환해 `calculateAdjacentLegTimes()` 호출.
2. 서비스는 인접 쌍을 생성하고, 쌍마다 가상 스레드에서 `CompletableFuture.supplyAsync(..., executor)`로
   `KakaoDirectionsClient.findRoute()`를 호출한다.
3. 각 future에 `.exceptionally()`를 걸어, 실패 시 해당 구간만 `RouteLeg(success=false, ...)`로 대체한다.
4. `CompletableFuture.allOf(...).join()`으로 전부 기다린 뒤, 원래 인접 쌍 순서대로 `List<RouteLeg>`를 반환한다.

가상 스레드 executor는 `DataExtractEmbeddingProcessor`와 동일하게 호출마다
`try (var executor = Executors.newVirtualThreadPerTaskExecutor())`로 생성한다(가상 스레드 executor는
스레드를 미리 풀링하지 않으므로 매 호출 생성 비용이 낮고, 기존 코드베이스 패턴과 일관적이다).

## 동시성 접근 비교

- A. 순차 호출 — 가장 단순하지만 5개 구간이면 지연이 그대로 누적된다.
- B. 요청마다 가상 스레드 executor를 만들고 `Future`로 결과를 모은다 — 기존 `DataExtractEmbeddingProcessor` 패턴.
- **C (채택). B와 동일한 동시성 모델에 `CompletableFuture` + `.exceptionally()`를 사용** — 구간별 실패 격리
  코드가 `Future.get()` try/catch 반복문보다 간결하다.

## 에러 처리 및 범위

- 구간 하나가 실패(4xx/5xx/timeout)해도 전체 호출을 실패시키지 않고 해당 구간만 `success=false`로 표시한다.
- 재시도, Redis 캐싱은 이번 스캐폴딩 범위에서 제외한다(YAGNI). 아직 이 컴포넌트를 호출하는 추천 로직이
  없어 실제 트래픽 패턴을 알 수 없으므로, 추천 기능이 붙은 뒤 필요성이 확인되면 추가한다.
- 인증/네트워크 오류는 로그만 남기고 해당 구간을 실패로 표시한다(전체 예외 전파하지 않음).

## 설정

- 신규 환경변수 `KAKAO_MOBILITY_REST_API_KEY`가 필요하다(기존 `KAKAO_ADMIN_KEY`/`KAKAO_CLIENT_ID`와는
  다른, 카카오모빌리티용 REST API 키). 실제 값은 이번 스캐폴딩 이후 `.env`에 추가한다.
- `departure_time`은 서비스 호출 시점 기준으로 생성한다(카카오 future/directions API 요구 파라미터,
  형식 `yyyyMMddHHmm`).

## 테스트

- `KakaoDirectionsClient`: `RestClient` 테스트용 mock server로 단건 호출/응답 파싱을 검증한다.
- `RouteLegTimeService`: `KakaoDirectionsClient`를 모킹해 인접 쌍 생성 로직과 부분 실패 처리(`.exceptionally()`
  경로)를 검증한다.
- 이 스캐폴딩을 호출할 추천 기능이 아직 없으므로, 임시 컨트롤러는 만들지 않고 테스트로만 동작을 검증한다.

## 향후 연동 (범위 밖)

- 추천 서비스에서 `TouristSpot → RouteWaypoint` 변환 및 `RouteLegTimeService` 호출 연결.
- 트래픽 확인 후 Redis 캐싱(`origin_dest` 키, 짧은 TTL) 및 재시도/백오프 추가 여부 판단.
