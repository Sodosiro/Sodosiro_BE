# sodosiro

Spring Boot 4.1 / Java 21 기반 여행지 추천 백엔드.
공공데이터(TourAPI) 관광정보를 수집하고, pgvector 임베딩 기반으로 관광지를 추천한다.

> 📄 실행 방법 · 환경설정(.env) · 의존성 · 디렉토리 구조 · 데이터 모델 상세는 [docs/InitProject.md](docs/InitProject.md) 참고.

## 기술 스택

| 구분 | 사용 기술 |
|------|-----------|
| Language / Runtime | Java 21, Spring Boot 4.1 |
| Persistence | PostgreSQL, Spring Data JPA, QueryDSL 5.1 |
| Vector | pgvector (`hibernate-vector`) |
| Cache | Redis (non-reactive) |
| Auth | Spring Security, OAuth2 Client(Kakao), JWT(jjwt 0.12.6) |
| AI | Spring AI 2.0 |
| Infra (AWS) | S3 / SES / Secrets Manager / STS (AWS SDK v2) |
| Ops | Actuator, Prometheus |
| Docs | Spring REST Docs, Swagger (springdoc-openapi 3) |
| Container | Docker, docker-compose (app · PostgreSQL · Redis) |
