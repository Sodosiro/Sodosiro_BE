#!/usr/bin/env bash
# app을 배포한다. DB는 AWS RDS를 사용하며(로컬에 postgres 컨테이너를 띄우지 않는다),
# 스키마/시드 적용은 psql 클라이언트가 담긴 일회성 도커 컨테이너로 RDS에 접속해 수행한다.
# (EC2 호스트에 psql을 별도로 설치하지 않는다)
# 사용법: ./docker/deploy.sh   (프로젝트 루트 어디서 실행해도 됨)
set -euo pipefail

cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "오류: 프로젝트 루트의 .env 파일이 필요합니다. .env.example을 복사해 설정하세요." >&2
  exit 1
fi

required_vars=(
  DB_HOST
  DB_NAME
  DB_USERNAME
  DB_PASSWORD
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

db_host_value="$(grep -E '^DB_HOST=' .env | tail -n 1 | cut -d= -f2-)"
if [[ "$db_host_value" == "localhost" || "$db_host_value" == "127.0.0.1" ]]; then
  missing_vars+=("DB_HOST(RDS 엔드포인트로 설정 필요, 현재: $db_host_value)")
fi

if (( ${#missing_vars[@]} > 0 )); then
  echo "오류: .env에 다음 필수 환경 변수를 설정하세요: ${missing_vars[*]}" >&2
  echo "JWT_SECRET은 32바이트 이상인 Base64 값이어야 합니다. 예: openssl rand -base64 48" >&2
  exit 1
fi

DB_HOST="$db_host_value"
DB_PORT="$(grep -E '^DB_PORT=' .env | tail -n 1 | cut -d= -f2-)"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="$(grep -E '^DB_NAME=' .env | tail -n 1 | cut -d= -f2-)"
DB_USERNAME="$(grep -E '^DB_USERNAME=' .env | tail -n 1 | cut -d= -f2-)"
DB_PASSWORD="$(grep -E '^DB_PASSWORD=' .env | tail -n 1 | cut -d= -f2-)"

compose() {
  docker compose --env-file .env -f docker/docker-compose.prod.yml "$@"
}

# RDS에 접속하는 일회성 psql 클라이언트 컨테이너. EC2 호스트에는 Docker만 있으면 된다.
pg_client() {
  docker run --rm -i \
    -e PGHOST="$DB_HOST" \
    -e PGPORT="$DB_PORT" \
    -e PGUSER="$DB_USERNAME" \
    -e PGDATABASE="$DB_NAME" \
    -e PGPASSWORD="$DB_PASSWORD" \
    -v "$(pwd)/docker/postgres:/docker/postgres:ro" \
    postgres:16-alpine \
    "$@"
}

echo "==> [1/6] 인프라 확인/기동 (redis)"
compose up -d --wait redis

echo "==> [2/6] pgvector 확장 확인 (RDS: $DB_HOST)"
pg_client psql -w -v ON_ERROR_STOP=1 -c "CREATE EXTENSION IF NOT EXISTS vector;"

echo "==> [3/6] 스키마 적용"
pg_client psql -w -v ON_ERROR_STOP=1 < docker/postgres/000_initialize_travel_schema.sql

echo "==> [4/6] 비어 있는 테이블에만 CSV 시드 데이터 적재"
pg_client sh -s << 'CONTAINER_SHELL'
set -e
CSV_DIR=/docker/postgres/seed/csv
SEED_TABLES="area_code category sigungu_code tourist_spot spot_embedding spot_image spot_popularity spot_ai_recommendation etl_spot_state"

for t in $SEED_TABLES; do
  f="$CSV_DIR/$t.csv"
  [ -f "$f" ] || { echo "  [SKIP] $t (CSV 없음)"; continue; }

  # 배포를 재실행해도 기존 데이터와 사용자가 작성한 리뷰를 삭제하지 않는다.
  has_rows="$(psql -w -tAc "SELECT EXISTS (SELECT 1 FROM \"$t\" LIMIT 1)")"
  if [ "$has_rows" = "t" ]; then
    echo "  [SKIP] $t (기존 데이터 유지)"
    continue
  fi

  # CSV 헤더를 읽어 컬럼 순서를 자동 지정 (테이블 정의 순서와 달라도 안전)
  cols="$(head -n 1 "$f" | tr -d '\r"' | awk -F, '{for(i=1;i<=NF;i++) printf "\"%s\"%s",$i,(i<NF?",":"")}')"
  # POSIX sh의 echo는 \c/\e를 제어 문자로 해석할 수 있으므로 psql 메타 명령은 printf로 전달한다.
  printf '%s\n' "\copy $t ($cols) FROM '$f' CSV HEADER" \
    | psql -w -v ON_ERROR_STOP=1
  echo "  [LOAD] $t"
done

psql -w -c "
  SELECT setval(pg_get_serial_sequence('sigungu_code', 'id'), COALESCE((SELECT MAX(id) FROM sigungu_code), 1));
  SELECT setval(pg_get_serial_sequence('spot_image',   'id'), COALESCE((SELECT MAX(id) FROM spot_image),   1));
"
CONTAINER_SHELL

echo "==> [5/6] 기존 app 컨테이너 및 이미지 교체"
APP_IMAGE_IDS="$(compose images -q app 2>/dev/null || true)"
compose stop app 2>/dev/null || true
compose rm -f app 2>/dev/null || true
while IFS= read -r image_id; do
  [[ -n "$image_id" ]] && docker image rm "$image_id" || true
done <<< "$APP_IMAGE_IDS"
compose build app

echo "==> [6/6] app 기동"
compose up -d --no-deps app

echo "==> 상태 & 최근 로그"
compose ps
echo "----- app logs (tail) -----"
compose logs --tail=40 app

echo
echo "완료. 실시간 로그: docker compose --env-file .env -f docker/docker-compose.prod.yml logs -f app"
APP_PORT="$(grep -E '^SERVER_PORT=' .env | tail -n 1 | cut -d= -f2-)"
APP_PORT="${APP_PORT:-8081}"
echo "Swagger UI: http://localhost:${APP_PORT}/swagger-ui/index.html"
