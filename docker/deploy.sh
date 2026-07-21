#!/usr/bin/env bash
# app만 교체 배포한다. postgres/redis는 없을 때만 기동하며 기존 컨테이너는 재시작하지 않는다.
# 사용법: ./docker/deploy.sh   (프로젝트 루트 어디서 실행해도 됨)
set -euo pipefail

# 스크립트 위치 기준으로 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "오류: 프로젝트 루트의 .env 파일이 필요합니다. .env.example을 복사해 설정하세요." >&2
  exit 1
fi

required_vars=(
  JWT_SECRET
  KAKAO_CLIENT_ID
  KAKAO_CLIENT_SECRET
  KAKAO_NATIVE_CLIENT_ID
  KAKAO_ADMIN_KEY
  OPENAI_API_KEY
)
missing_vars=()
for var_name in "${required_vars[@]}"; do
  value="$(grep -E "^${var_name}=" .env | tail -n 1 | cut -d= -f2-)"
  if [[ -z "$value" || "$value" == "sk-..." || "$value" == "changeme" ]]; then
    missing_vars+=("$var_name")
  fi
done

if (( ${#missing_vars[@]} > 0 )); then
  echo "오류: .env에 다음 필수 환경 변수를 설정하세요: ${missing_vars[*]}" >&2
  echo "JWT_SECRET은 32바이트 이상인 Base64 값이어야 합니다. 예: openssl rand -base64 48" >&2
  exit 1
fi

compose() {
  docker compose --env-file .env -f docker/docker-compose.yml "$@"
}

echo "==> [1/6] 인프라 확인/기동 (postgres, redis)"
# 이미 실행 중인 컨테이너는 그대로 두고, 없거나 중지된 경우에만 시작한다.
compose up -d --wait postgres redis

echo "==> [2/6] 현재 도메인 스키마 적용"
# 호스트에 psql 설치가 없어도 되도록 PostgreSQL 컨테이너 안에서 실행한다.
# SQL은 IF NOT EXISTS 기반이므로 매 배포 시 실행해도 기존 데이터에는 영향을 주지 않는다.
compose exec -T postgres sh -ec '
  if ! psql -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname = '\''$APP_DB_NAME'\''" | grep -qx 1; then
    createdb -U "$POSTGRES_USER" "$APP_DB_NAME"
  fi
'
compose exec -T postgres sh -ec 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$APP_DB_NAME"' \
  < docker/postgres/000_initialize_travel_schema.sql

echo "==> [3/6] 기존 app 컨테이너 중지 및 삭제"
# 컨테이너를 삭제하기 전에 현재 app 이미지 ID를 보관한다.
APP_IMAGE_IDS="$(compose images -q app 2>/dev/null || true)"
compose stop app 2>/dev/null || true
compose rm -f app 2>/dev/null || true

echo "==> [4/6] 기존 app 이미지 삭제"
while IFS= read -r image_id; do
  if [[ -n "$image_id" ]]; then
    docker image rm "$image_id" || true
  fi
done <<< "$APP_IMAGE_IDS"

echo "==> [5/6] app 이미지 빌드"
compose build app

echo "==> [6/6] app 기동 (--no-deps: 인프라 재시작 안 함)"
compose up -d --no-deps app

echo "==> 상태 & 최근 로그"
compose ps
echo "----- app logs (tail) -----"
compose logs --tail=40 app

echo
echo "완료. 실시간 로그: docker compose --env-file .env -f docker/docker-compose.yml logs -f app"
APP_PORT="$(grep -E '^SERVER_PORT=' .env | tail -n 1 | cut -d= -f2-)"
APP_PORT="${APP_PORT:-8080}"
echo "Swagger UI: http://localhost:${APP_PORT}/swagger-ui/index.html"
