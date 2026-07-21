# 프로젝트 문서

Spring Boot 4.1 / Java 21 기반 여행지 추천 백엔드의 실행 방법 · 환경설정 · 의존성 · 구조 정리 문서.

---

## 1. 실행 방법

### 사전 요구사항
- JDK 21
- PostgreSQL 16+ (`pgvector` 확장 필요)
- Redis

```sql
-- PostgreSQL 에서 pgvector 확장 활성화 (최초 1회)
CREATE EXTENSION IF NOT EXISTS vector;
```

### 실행

```bash
# 1. 환경변수 파일 준비
cp .env.example .env      # 값을 실제 환경에 맞게 수정

# 2. 실행
./gradlew bootRun
```

또는 IntelliJ 에서 `SodosiroApplication` 을 직접 실행해도 된다.

### Docker 로 실행 (권장, Linux/Ubuntu 기준)

app + PostgreSQL(pgvector) + Redis 를 한 번에 띄운다. 도커 관련 파일은 모두 `docker/` 에 있다.

```bash
# 프로젝트 루트에서 실행 (상대 경로는 docker/ 기준으로 해석됨)
docker compose -f docker/docker-compose.yml up --build -d

# 상태 확인
docker compose -f docker/docker-compose.yml ps

# 로그
docker compose -f docker/docker-compose.yml logs -f app

# 종료 (데이터 볼륨 유지)
docker compose -f docker/docker-compose.yml down

# 종료 + 데이터 볼륨까지 삭제
docker compose -f docker/docker-compose.yml down -v
```

구성 요소:

| 서비스 | 이미지 | 포트 | 볼륨(마운트) |
|--------|--------|------|--------------|
| `app` | 로컬 빌드(`docker/Dockerfile`) | `${SERVER_PORT:-8080}` | `../logs → /app/logs` (로그) |
| `postgres` | `pgvector/pgvector:pg16` | 5432 | `postgres-data`(데이터) + `./postgres/init.sql`(확장 자동설치) |
| `redis` | `redis:7-alpine` | 6379 | `redis-data`(데이터) |

- 컨테이너 안에서는 `.env` 대신 compose 의 `environment` 값이 주입된다 (`DB_HOST=postgres`, `REDIS_HOST=redis`).
- `SERVER_PORT`는 Spring 기동 포트와 Docker의 호스트 공개 포트에 동일하게 적용된다. 기본값은 `8080`이며, 개발자별로 루트 `.env`에서 변경할 수 있다.
- `postgres/init.sql` 이 최초 기동 시 `CREATE EXTENSION vector` 를 자동 실행하므로 수동 확장 설치가 필요 없다.
- `.env` 는 `.dockerignore`(`docker/Dockerfile.dockerignore`)로 이미지에서 제외된다.
- DB 이름·계정은 프로젝트 루트 `.env` 값으로 덮어쓸 수 있다 (compose 가 substitution).

디렉토리:

```
docker/
├─ Dockerfile                 # 멀티스테이지 빌드 (gradle → JRE)
├─ Dockerfile.dockerignore    # BuildKit 전용 ignore
├─ docker-compose.yml         # app + postgres + redis
└─ postgres/
   └─ init.sql                # pgvector 확장 자동 생성
```

---

## 2. 환경변수(.env) 적용 방법

DB · Redis 접속 정보는 프로젝트 루트의 `.env` 파일로 주입한다.
Spring Boot 4 의 `spring.config.import` 기능으로 로드하므로 **별도 라이브러리가 필요 없다.**

`application.yaml`:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]   # .env 를 properties 로 읽는다
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

- `optional:` → `.env` 가 없어도 앱이 실패하지 않는다 (운영 서버는 실제 OS 환경변수 사용)
- `[.properties]` → 확장자 없는 `.env` 를 properties 로더로 파싱하라는 힌트
- `file:` → 클래스패스가 아닌 프로젝트 루트 파일시스템에서 로드

동작 흐름:

```
.env  →  spring.config.import 가 로드  →  ${DB_HOST} 등 placeholder 치환
```

> `.env` 는 `.gitignore` 대상 (커밋 금지). 필요한 변수 목록은 `.env.example` 참고.

---

## 3. 의존성 정보

`build.gradle` 기준. 버전이 비어있는 항목은 Spring Boot BOM 이 관리한다.

### 플러그인
| 플러그인 | 버전 | 용도 |
|----------|------|------|
| `org.springframework.boot` | 4.1.0 | Spring Boot |
| `io.spring.dependency-management` | 1.1.7 | 의존성 버전 관리(BOM) |
| `org.asciidoctor.jvm.convert` | 4.0.2 | REST Docs → HTML 문서화 |

### BOM
| BOM | 버전 |
|-----|------|
| `spring-ai-bom` | 2.0.0 |
| `awssdk:bom` | 2.47.3 |

### 라이브러리
| 분류 | 의존성 | 버전 | 용도 |
|------|--------|------|------|
| Web | `spring-boot-starter-webmvc` | BOM | REST API (MVC) |
| JPA | `spring-boot-starter-data-jpa` | BOM | ORM |
| DB Driver | `postgresql` | BOM | PostgreSQL 드라이버 |
| Vector | `hibernate-vector` | BOM | pgvector 타입 매핑 |
| Query | `querydsl-jpa` (jakarta) | 5.1.0 | 타입세이프 동적 쿼리 |
| Cache | `spring-boot-starter-data-redis` | BOM | Redis (non-reactive) |
| Security | `spring-boot-starter-security` | BOM | 인증/인가 |
| OAuth2 | `spring-boot-starter-oauth2-client` | BOM | Kakao 로그인 |
| JWT | `jjwt-api` / `jjwt-impl` / `jjwt-jackson` | 0.12.6 | JWT 토큰 |
| AI | `spring-ai-bom` | 2.0.0 | Spring AI (모델 스타터 별도 추가) |
| AWS | `s3` / `ses` / `secretsmanager` / `sts` | BOM(2.47.3) | S3·메일·시크릿·STS |
| Monitoring | `spring-boot-starter-actuator` + `micrometer-registry-prometheus` | BOM | 모니터링 |
| Docs | `spring-restdocs-mockmvc` + `spring-restdocs-asciidoctor` | BOM | API 문서화 |
| Swagger | `springdoc-openapi-starter-webmvc-ui` | 3.0.3 | OpenAPI/Swagger UI (Spring Boot 4 호환) |
| Lombok | `lombok` | BOM | 보일러플레이트 제거 |

> **Spring AI 모델 스타터**는 사용할 모델에 따라 별도 추가 필요
> (예: `spring-ai-starter-model-openai`)

---

## 4. 디렉토리 구조

**도메인형(Domain-based)** 패키지 구조. 각 도메인은 `controller / service / repository / entity / dto` 로 나눈다.

```
sodosiro/
├─ src/
│   ├─ main/
│   │   ├─ java/com/sodosiro/
│   │   │   ├─ SodosiroApplication.java
│   │   │   │
│   │   │   ├─ domain/                  # 비즈니스 도메인
│   │   │   │   ├─ member/              #   회원
│   │   │   │   ├─ auth/                #   인증 (Kakao · 토큰)
│   │   │   │   └─ travel/              #   여행지 (관광정보 · 추천)
│   │   │   │       ├─ controller/
│   │   │   │       │   └─ dto/         #   요청/응답 DTO (컨트롤러 전용)
│   │   │   │       ├─ service/
│   │   │   │       ├─ repository/
│   │   │   │       └─ entity/          #   ← 현재까지 구현
│   │   │   │
│   │   │   └─ global/                  # 전역 공통
│   │   │       ├─ config/              #   SecurityConfig · CorsConfig · SwaggerConfig
│   │   │       ├─ security/            #   jwt · handler · principal · service
│   │   │       ├─ exception/
│   │   │       ├─ response/
│   │   │       └─ util/
│   │   │
│   │   └─ resources/
│   │       └─ application.yaml
│   │
│   └─ test/java/com/sodosiro/
│
├─ docker/                              # 도커 구성 (Dockerfile · compose · init.sql)
├─ docs/InitProject.md                  # 본 문서
├─ build.gradle
├─ settings.gradle
├─ .env.example                         # 환경변수 템플릿 (커밋됨)
├─ .env                                 # 실제 환경변수 (커밋 제외)
├─ .gitignore
└─ README.md
```

### travel 도메인 엔티티
| 엔티티 | 테이블 | 설명 |
|--------|--------|------|
| `AreaCode` | `area_code` | 지역코드 (광역시도) |
| `SigunguCode` | `sigungu_code` | 시군구코드 (IDENTITY PK + `(area_code, sigungu_code)` UNIQUE) |
| `Category` | `category` | 분류코드 계층 (자기참조) |
| `TouristSpot` | `tourist_spot` | 관광지 기본정보 |
| `SpotImage` | `spot_image` | 관광지 이미지 1:다 (IDENTITY PK + `(content_id, order)` UNIQUE) |
| `SpotEmbedding` | `spot_embedding` | 임베딩 벡터 (pgvector, 1:1) |

> 각 엔티티는 Hibernate `@Comment` 로 PostgreSQL 테이블/컬럼 주석을 단다.
> `ddl-auto: create` 시 `COMMENT ON TABLE/COLUMN` DDL 이 생성되어 DB 툴(psql `\d+`, DBeaver 등)에서 확인 가능하다.

---

## 5. API 문서 (Swagger)

springdoc-openapi 로 자동 생성된다. 앱 기동 후:

| 항목 | URL |
|------|-----|
| Swagger UI | `http://localhost:${SERVER_PORT:-8080}/swagger-ui/index.html` |
| OpenAPI 스펙(JSON) | `http://localhost:${SERVER_PORT:-8080}/v3/api-docs` |

설정: `global/config/SwaggerConfig` (제목·설명·버전), 접근 허용은 `SecurityConfig` 에서 처리.
