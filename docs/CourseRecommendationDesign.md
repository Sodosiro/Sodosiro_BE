# AI 코스 추천 설계 문서

여행 날짜 · 여행스타일 · 꼭 가고 싶은 곳 · AI 한마디(자유 텍스트)를 입력받아 일자별 관광지 코스를 추천하고,
프론트에서 장소/순서를 조정해 확정하면 이동수단(자차/대중교통)에 맞는 구간 데이터를 내려주는 기능.
로그인 사용자 전용이며, 추천 결과는 생성 시점에 바로 draft로 저장되어 확정 전에 앱을 나갔다 들어와도
재계산(AI 토큰 재소모) 없이 이어갈 수 있다.

---

## 1. 요구사항 요약

1. **이동수단**: 코스를 확정할 때 자차/대중교통을 선택하면 그에 맞는 이동 데이터를 내려준다.
2. **여행스타일**: 식당·카페·쇼핑·관광지·자연·액티비티 중 최대 2개를 선택하면 해당 카테고리를 메인으로 추천한다.
3. **꼭 가고 싶은 곳**: 사용자가 관광지 하나를 직접 지정하면(선택 사항) 반드시 코스에 포함한다.
4. **AI 한마디**: 자유 텍스트를 임베딩해 관광지 임베딩과 코사인 유사도로 비교, 적절한 관광지를 추천한다.
5. **일정 받기**: 여행 날짜를 받아 해당 날짜의 요일이 휴무일(`restdate`)에 포함되면 그 관광지를 제외한다.
6. **임시저장**: 추천 API 호출 자체가 AI 임베딩 비용을 수반하므로, 확정 여부와 무관하게 결과를 바로 저장해
   앱 재진입 시 복원할 수 있어야 한다 (사용자당 미확정 draft 1개).

## 2. API 개요

| API | Method & Path | 역할 | 저장 여부 |
|---|---|---|---|
| 코스 추천 | `POST /api/v1/courses/recommendations` | 조건에 맞는 일자별 관광지 코스 생성 | **있음** — draft로 저장, 기존 미확정 draft는 삭제 후 교체 |
| 코스 확정 (자차) | `POST /api/v1/courses/confirm/car` | 확정된 순서에 카카오 길찾기 기반 구간 데이터 부여 | **있음** — 기존 draft를 확정 상태로 갱신 (구간 데이터 자체는 저장 안 함) |
| 코스 확정 (대중교통) | `POST /api/v1/courses/confirm/public-transport` | 확정된 순서에 ODsay 기반 대중교통 구간 데이터 부여 | **있음** — 위와 동일 |

**흐름**: `recommendations` 호출 → 응답으로 `courseId` + 코스가 내려옴 → 프론트에서 장소/순서 편집
(이 사이 앱을 나갔다 들어와도 저장된 draft이므로 유실되지 않음) → "확정하기" 시 `courseId`와 함께
`confirm/car` 또는 `confirm/public-transport` 호출 → 같은 row가 확정 상태로 갱신됨.
세 API 모두 로그인 사용자 전용이며(`@LoginUser`), 카카오/ODsay 구간 데이터 자체는 저장하지 않고 매번
재계산해서 응답만 내려준다.

---

## 3. API 1 — 코스 추천 (`POST /courses/recommendations`)

### 요청/응답

```json
// Request
{
  "startDate": "2026-08-15",
  "endDate": "2026-08-17",
  "travelStyles": ["CAFE", "NATURE"],   // 최대 2개, 선택
  "mustVisitContentId": 12345,          // 선택
  "aiMessage": "조용하고 바다가 보이는 곳 위주로"  // 선택
}
```
```json
// Response
{
  "courseId": 42,
  "days": [
    {
      "day": 1,
      "date": "2026-08-15",
      "spots": [
        { "contentId": 12345, "title": "...", "mapX": .., "mapY": .., "category": 5, "mustVisit": true },
        { "contentId": 222, "...": "...", "category": 1, "mustVisit": false },
        ...
      ]
    }
  ]
}
```

### 처리 파이프라인

```
1. 날짜 검증 (endDate >= startDate)
2. 여행 기간의 날짜 리스트 생성 (2박3일 → 3일)
3. mustVisitContentId가 있으면 조회(404) 후 "휴무 아닌 첫 날짜"에 배정
4. 후보 풀(candidatePool) 구성          ─┐
5. 날짜별로 하루 5슬롯 채우기            │  아래 4~6절에서 상세 설명
6. 같은 날 안에서 동선(위경도) 근접 정렬 ─┘
7. 결과를 Course draft로 저장하고 courseId를 응답에 포함 (7절)
```

### 4. 후보 풀 구성 — 카테고리는 "필터"가 아니라 "가중치"

처음에는 `임베딩 유사도 후보`와 `카테고리 후보`를 완전히 독립된 두 풀로 만들어 단순 합집합했으나,
이렇게 하면 **여행스타일과 무관하게 AI 한마디와만 비슷한 곳**이 섞여 들어가는 문제가 있었다.
→ 여행스타일과 AI 한마디가 둘 다 있으면 **두 조건을 동시에** 반영해야 한다.

그래서 카테고리 조건을 SQL `WHERE`로 걸러내는 대신, **임베딩 유사도 검색 자체의 정렬 우선순위**로 사용한다
(`SpotEmbeddingQueryRepositoryImpl.findNearestContentIds`):

```sql
SELECT e.content_id FROM spot_embedding e
JOIN tourist_spot s ON s.content_id = e.content_id
ORDER BY
  (CASE WHEN s.category IN (:categoryCodes) THEN 0 ELSE 1 END),  -- 선택 카테고리를 앞쪽으로
  e.embedding <=> CAST(:queryVector AS vector)                    -- 그 안에서 유사도순
LIMIT :limit
```

전체 카테고리를 대상으로 검색하되, 선택한 카테고리에 속한 관광지가 먼저 오도록 순위를 끌어올린다.
카테고리 밖의 관광지도 유사도가 높으면 후보에 남는다 (완전 배제 아님).

이 단계는 `EmbeddingModel.embed(aiMessage)`로 텍스트를 벡터화하는 것까지만 AI를 사용하고, 이후는
전부 pgvector 코사인 유사도 검색 + 결정론적 알고리즘(슬롯 채우기, 동선 정렬)이다. `ChatModel`(GPT
채팅 완성) 기반의 프롬프트 호출은 이 파이프라인에 없다 — 관광지 상세 페이지의 "AI 추천 이유" 문구
(`SpotAiRecommendationService`)와는 별개 기능이며, 코스 추천 결과에 자연어 가이드 문구를 붙이는 건
아직 하지 않는다 (7절 후속 과제 참고).

`buildCandidatePool`은 이렇게 만든 임베딩 후보 뒤에 카테고리 전용 후보(평점순), 전체 인기순 폴백을 이어붙여
중복 제거 후 하나의 순위 리스트로 만든다.

| 소스 | 조건 | 정렬 | 용도 |
|---|---|---|---|
| 임베딩 후보 (최대 60개) | `aiMessage` 있을 때만 | 카테고리 가중치 → 코사인 유사도 | 1순위 |
| 카테고리 후보 (최대 200개) | `travelStyles` 있을 때만 | 평점(`avgRating`) 내림차순 | 보강 |
| 인기 폴백 (최대 200개) | 항상 | 평점 내림차순 | 최종 안전망 |

### 5. 하루 5슬롯 = 필수 3 + 자율 2

단순히 후보 풀 상위 5개를 뽑으면 카페나 식당 없이 관광지만 5개가 뽑히는 경우가 생길 수 있어,
하루 일정을 슬롯 구조로 고정했다.

| 슬롯 | 개수 | 카테고리 |
|---|---|---|
| 필수 — 식당 | 1 | category 1 |
| 필수 — 카페 | 1 | category 2 |
| 필수 — 관광/자연/액티비티/쇼핑 | 1 | category 3~6 |
| 자율 | 2 | 제한 없음 (후보 풀 상위 순서대로) |

`RequiredSlotGroup` enum(`RESTAURANT` / `CAFE` / `EXPERIENCE`)이 이 세 그룹을 표현한다.
(category 7=숙박은 필수 슬롯 그룹에 속하지 않는다.)

**슬롯 채우기 순서** (`pickForDay` → `pickForGroup` / `pickFreeSlots`):
1. 필수 그룹별로 후보 풀에서 "그 그룹 카테고리 + 아직 안 쓴 곳 + 그날 휴무 아닌 곳" 중 첫 번째를 선택
2. 후보 풀 안에 없으면(예: 후보 풀 상위 260개 안에 카페가 하나도 없는 경우) `TouristSpotRepository.findTop200ByCategoryInOrderByAvgRatingDesc(그룹 카테고리)`로 직접 조회해 보강
3. 그래도 없으면 `400 Bad Request` (`"카페 카테고리에 해당하는 관광지가 부족합니다."`)
4. 필수 3개(또는 mustVisit이 있으면 2개, 아래 참고)를 채운 뒤, 후보 풀 상위 순서 그대로 자율 슬롯 2개를 채움

**mustVisit이 배정된 날**: `RequiredSlotGroup.groupOf(mustVisitSpot.getCategory())`로 mustVisit이 어느 그룹에 속하는지 확인하고,
그 그룹은 "이미 채워진 것"으로 간주해 건너뛴다. 즉 그날은 필수 2개 + 자율 2개(총 4개)만 더 뽑고 mustVisit이 나머지 1자리를 차지한다.
전체 여행 기간에서 같은 관광지가 중복 배정되지 않도록 `usedContentIds`를 날짜 간 공유한다.

### 6. 동선 정렬 (`orderByProximity`)

같은 날 뽑힌 5곳을 mapX/mapY 기준 유클리드 거리로 그리디 최근접 이웃 정렬한다.
mustVisit이 있으면 그곳을 시작점으로 고정하고, 없으면 임의의 한 곳에서 시작해 가장 가까운 곳을 차례로 이어 붙인다.
완전한 최단 경로(TSP)는 아니고, 동선이 크게 튀는 것을 막는 수준의 근사치다.

**주의**: 여기서 정해지는 순서는 어디까지나 추천 시점의 *권장 순서*다. 실제 방문 순서를 GPS 인증
기반으로 유동적으로 추적하는 기능은 별도 후속 과제다 (7절 참고).

### 7. 추천 결과 저장 (draft)

계산이 끝나면 `CourseRecommendationService.saveDraft`가 다음을 수행한다:

1. `CourseRepository.findByUserIdAndIsConfirmedFalse(userId)`로 기존 미확정 draft를 찾아 있으면 삭제
   (사용자당 미확정 draft는 항상 1개만 유지)
2. 계산된 `days`를 `Course.DaySnapshot`/`Course.SpotSnapshot`으로 변환해 새 `Course`(`isConfirmed=false`)로 저장
3. 저장된 `Course.id`를 응답의 `courseId`로 내려줌

데이터 모델은 8절에서 자세히 다룬다.

---

## 4. API 2 — 코스 확정 (`confirm/car`, `confirm/public-transport`)

### 요청

```json
{
  "courseId": 42,
  "days": [
    { "day": 1, "contentIds": [12345, 222, 333, 444, 555] },
    { "day": 2, "contentIds": [666, 777, 888, 999, 111] }
  ]
}
```
프론트에서 추천 결과를 받아 장소/순서를 편집한 뒤 확정한 최종 상태를, 추천 시 받은 `courseId`와 함께 보낸다.

### 처리

1. `CourseRepository.findByIdAndUserId(courseId, userId)`로 draft 조회 — 없거나 다른 사용자 소유면
   `404` (`"코스를 찾을 수 없습니다."`). 여기서 소유자 검증이 끝난다.
2. 요청에 등장한 모든 `contentId`를 한 번에 조회해 존재 확인 (없으면 404)
3. 조회한 스팟 정보로 `days`를 재구성해 `Course.confirm(transportMode, days)` 호출
   — `isConfirmed=true`, `transportMode` 세팅, `mustVisit`은 저장돼 있던 `mustVisitContentId`와
   비교해 다시 표시, 각 day의 `date`는 draft에 저장된 값을 그대로 이어받음(재확정도 허용, 별도 상태
   검증 없이 덮어씀)
4. 각 Day를 `RouteWaypoint(id, mapX, mapY)` 리스트로 변환 (요청 순서를 그대로 유지, 재정렬하지 않음)
5. 기존 `route` 도메인의 `RouteCalculationService.calculateAdjacentRoutes(waypoints, mode)` 호출
   - `CAR` → `KakaoDirectionsClient`로 인접 구간별 카카오 길찾기 조회
   - `PUBLIC_TRANSPORT` → `OdsayDirectionsClient`로 인접 구간별 버스/지하철 경로 조회
6. `AdjacentRouteResult`는 sealed interface(`Car` / `PublicTransport`)라 `instanceof` 패턴 매칭으로 타입을 좁혀 꺼낸다

구간(경로) 데이터 자체는 이 과정에서 DB에 저장하지 않는다 — 외부 API 응답이라 금방 stale해지고,
자차/대중교통 응답 구조가 근본적으로 달라 저장 모델을 새로 만들 가치가 지금은 없다고 판단했다.

### 응답

- **car**: `Day`별 `RouteLeg`(구간 소요시간·거리) 리스트
- **public-transport**: `Day`별 `OdsayRouteDetailResponse`(총 시간/거리/요금/좌표경로/구간정보) 리스트

자차와 대중교통은 응답 구조가 근본적으로 달라(단순 시간/거리 vs 노선·구간 상세) 엔드포인트와 응답 DTO를 처음부터 분리했다.

---

## 5. 패키지 구조

```
domain/course/
  controller/
    CourseController.java
    dto/
      CourseRecommendRequest.java
      CourseRecommendResponse.java               # DayCourse, RecommendedSpot (courseId 포함)
      DayConfirm.java                            # confirm 두 API 공용
      CourseConfirmCarRequest / Response.java     # courseId 포함
      CourseConfirmPublicTransportRequest / Response.java
  constants/
    TravelStyle.java                             # enum, category(1~6) 매핑 (controller/dto에서 이동)
  entity/
    Course.java                                  # draft/확정 공용 엔티티, days는 JSONB
  repository/
    CourseRepository.java                        # findByUserIdAndIsConfirmedFalse, findByIdAndUserId
  service/
    CourseRecommendationService.java             # API1 핵심 로직 + draft 저장
    CourseConfirmationService.java               # API2, draft 확정 갱신 + route 도메인 위임

domain/travel/repository/
  SpotEmbeddingQueryRepository.java              # pgvector 코사인 유사도
  SpotEmbeddingQueryRepositoryImpl.java
  TouristSpotRepository.java                     # findTop200By... 2개 메서드 추가
```

`route` 도메인(`RouteCalculationService`, `TransportMode`, `RouteWaypoint` 등)은 기존 구현을 그대로 재사용했다.

---

## 6. 데이터 모델 — `Course`

`Course` / `CourseDay` / `CourseSpot` 3테이블 정규화를 검토했으나 채택하지 않았다. 이 데이터는 항상
**통째로 쓰고 통째로 갈아끼우는** 접근 패턴만 갖는다(추천이 하루~여러 날 일정을 한 번에 생성, 확정이
프론트 편집 결과로 통째로 덮어씀). 개별 day/spot을 따로 쿼리하거나 다른 도메인이 JOIN할 요구사항이
없어 정규화된 자식 테이블이 이점 없이 복잡도만 늘린다. `TouristSpot.detailInfo` 컬럼에 이미 있던
JSONB 저장 선례(`@JdbcTypeCode(SqlTypes.JSON)` + `columnDefinition = "jsonb"`, 추가 라이브러리
불필요)를 재사용해 `Course` 테이블 하나에 일자별 코스 전체를 저장한다.

| 컬럼 | 타입 | 설명 |
|---|---|---|
| `id` | `Long` (IDENTITY) | PK |
| `user_id` | `Long` | 소유자 (`users` 참조, DB 제약 없음 — `Review`/`SpotLike`와 동일한 FK 컨벤션) |
| `start_date` / `end_date` | `LocalDate` | 여행 기간 |
| `travel_styles` | `String` | `TravelStyle` 최대 2개를 콤마로 이어붙인 문자열 (예: `"CAFE,NATURE"`) |
| `must_visit_content_id` | `Long`, nullable | 필수 방문지 |
| `ai_message` | `String` (text), nullable | AI 한마디 원문 |
| `transport_mode` | `String` (`EnumType.STRING`), nullable | 확정 전엔 null |
| `is_confirmed` | `Boolean` | 확정 여부 |
| `days` | JSONB | `Course.DaySnapshot`/`Course.SpotSnapshot` 리스트 (컨트롤러 DTO와는 별도 타입 — 엔티티가 controller.dto에 의존하지 않도록 분리) |
| `created_at` / `updated_at` | `LocalDateTime` | 생성/수정 일시 |

엔티티는 상태 변화를 `createDraft(...)`(추천 시 생성)와 `confirm(...)`(확정 시 전환) 두 메서드로만
캡슐화한다.

---

## 7. 주요 설계 결정 기록

| 결정 | 이유 |
|---|---|
| 추천 결과를 `Course`에 draft로 저장, 사용자당 미확정 draft 1개 유지 | 추천 호출 자체가 AI 토큰 비용을 수반해 확정 여부와 무관하게 이미 의미 있는 데이터임. 앱 재진입 시 재계산 없이 복원하기 위함 |
| `Course` 테이블 하나에 `days`를 JSONB로 저장 (자식 테이블 분리 안 함) | day/spot을 항상 통째로 읽고 쓰는 접근 패턴이라, 정규화된 자식 테이블은 복잡도만 늘림 |
| 구간(경로) 데이터는 확정 시에도 저장하지 않음 | 외부 API 응답이라 금방 stale해지고, 자차/대중교통 응답 구조가 근본적으로 달라 저장 모델을 새로 만들 가치가 아직 없음 |
| 카테고리를 SQL 필터가 아닌 정렬 가중치로 처리 | 여행스타일과 AI 한마디를 "둘 중 하나"가 아니라 "둘 다" 반영해야 한다는 요구사항 때문 |
| 하루를 필수 3 + 자율 2 슬롯으로 고정 | 카테고리 필터링 결과만으로는 식당·카페 없이 관광지 5개로 쏠릴 수 있어, 최소 다양성을 보장 |
| pgvector 코사인 검색을 EntityManager 네이티브 쿼리로 구현 | Querydsl이 `<=>` 연산자를 지원하지 않음 |
| 자차/대중교통 확정 API를 엔드포인트째로 분리 | `RouteLeg`(시간/거리)와 `OdsayRouteDetailResponse`(노선/구간/요금)는 필드 구조가 근본적으로 달라 하나의 DTO로 억지로 묶지 않음 (route 도메인의 `AdjacentRouteResult` sealed interface 설계와 동일한 철학) |
| mustVisit을 "휴무 아닌 첫 날짜"에 배치 | 여행 기간 내내 휴무면 첫째 날에 배치(최선 노력). 사용자가 명시적으로 요청한 곳이라 카테고리 슬롯과 무관하게 항상 포함 |
| `TravelStyle`을 `controller/dto`에서 `constants`로 이동 | 엔티티(`Course`)가 이 enum을 참조해야 하는데, 엔티티가 controller 계층 DTO 패키지에 의존하면 레이어 위반이라 `ReviewVisitType`과 같은 위치 컨벤션으로 옮김 |

## 8. 알려진 제약 / 후속 개선 여지

- **후보 부족 시 400 에러**: 특정 요일 + 특정 카테고리 조합에서 DB에 관광지가 절대적으로 부족하면 추천 자체가 실패한다. 데이터가 충분히 채워진 뒤 실제 요청으로 검증이 필요하다.
- **동선 정렬은 근사치**: 그리디 최근접 이웃이라 완전한 최단 경로는 아니다.
- **여행 일수/최대 길이 제한 없음**: 과도하게 긴 여행 기간에 대한 방어 로직은 아직 없다.
- **통합 테스트 미실행**: `/recommendations` → `/confirm/car`(또는 `public-transport`) 흐름을 실제로 기동해서 호출해보지 않았다. 이 레포에는 아직 테스트 코드 자체가 거의 없다.
- **내 코스 목록/재조회 API 없음**: 확정된 `Course` row는 DB에 계속 남지만, 이를 조회하는 API는 아직 없다. 나중에 "내 여행 이력" 기능을 붙일 때 이 테이블을 그대로 활용할 수 있다.
- **동시에 여러 draft 불가**: 사용자가 두 여행을 동시에 기획할 수 없다. 필요해지면 `findByUserIdAndIsConfirmedFalse`를 다건 조회로 바꾸고 프론트에 draft 선택 UI가 추가돼야 한다.
- **확정된 코스가 무한히 쌓임**: 정리 정책(보관 기간, 소프트 삭제 등)은 아직 없다.
- **GPS 인증 기반 실제 방문 순서 추적은 별도 후속 기능**: `days`에 저장되는 순서는 추천/확정 시점의 권장 순서(동선 근접 정렬)일 뿐이며, 사용자가 실제로 GPS 인증하며 방문하는 순서는 이와 다를 수 있다. `review` 도메인의 GPS 인증(`ReviewGpsVerificationRequest`, `Review.verifyGpsVisit()`)과 유사한 방식으로 코스 도메인에도 추후 별도 설계가 필요하다.
- **ChatModel 기반 자연어 가이드 문구 없음**: 코스 추천은 임베딩 검색 + 결정론적 알고리즘으로만 동작하고, `SpotAiRecommendationService`처럼 "이 코스를 추천하는 이유"를 자연어로 생성하는 단계가 없다. 필요하면 `days` 계산 후 `ChatClient.prompt().system(...).user(...)` 호출을 추가해 코스 소개 문구를 붙이고, 이왕 저장 구조가 있으니 `Course.days`에 함께 저장해 재호출을 피할 수 있다.
