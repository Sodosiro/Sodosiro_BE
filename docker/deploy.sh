#!/usr/bin/env bash
# app만 교체 배포한다. postgres/redis는 없을 때만 기동하며 기존 컨테이너는 재시작하지 않는다.
# 사용법: ./docker/deploy.sh   (프로젝트 루트 어디서 실행해도 됨)
set -euo pipefail

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

echo "==> [1/7] 인프라 확인/기동 (postgres, redis)"
compose up -d --wait postgres redis

echo "==> [2/7] 스키마 적용"
compose exec -T postgres sh -ec '
  psql -w -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "SELECT 1" > /dev/null 2>&1 \
    || createdb -w -U "$POSTGRES_USER" "$POSTGRES_DB"
'
compose exec -T postgres sh -ec 'psql -w -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"' \
  < docker/postgres/000_initialize_travel_schema.sql

echo "==> [3/7] 비어 있는 테이블에만 CSV 시드 데이터 적재"
compose exec -T postgres sh -s << 'CONTAINER_SHELL'
set -e
CSV_DIR=/docker/postgres/seed/csv
SEED_TABLES="area_code category sigungu_code tourist_spot spot_embedding spot_image spot_popularity spot_ai_recommendation etl_spot_state"

for t in $SEED_TABLES; do
  f="$CSV_DIR/$t.csv"
  [ -f "$f" ] || { echo "  [SKIP] $t (CSV 없음)"; continue; }

  # 배포를 재실행해도 기존 데이터와 사용자가 작성한 리뷰를 삭제하지 않는다.
  has_rows="$(psql -w -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
    "SELECT EXISTS (SELECT 1 FROM \"$t\" LIMIT 1)")"
  if [ "$has_rows" = "t" ]; then
    echo "  [SKIP] $t (기존 데이터 유지)"
    continue
  fi

  # CSV 헤더를 읽어 컬럼 순서를 자동 지정 (테이블 정의 순서와 달라도 안전)
  cols="$(head -n 1 "$f" | tr -d '\r"' | awk -F, '{for(i=1;i<=NF;i++) printf "\"%s\"%s",$i,(i<NF?",":"")}')"
  # POSIX sh의 echo는 \c/\e를 제어 문자로 해석할 수 있으므로 psql 메타 명령은 printf로 전달한다.
  printf '%s\n' "\copy $t ($cols) FROM '$f' CSV HEADER" \
    | psql -w -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"
  echo "  [LOAD] $t"
done

psql -w -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "
  SELECT setval(pg_get_serial_sequence('sigungu_code', 'id'), COALESCE((SELECT MAX(id) FROM sigungu_code), 1));
  SELECT setval(pg_get_serial_sequence('spot_image',   'id'), COALESCE((SELECT MAX(id) FROM spot_image),   1));
"
CONTAINER_SHELL

echo "==> [4/7] 기존 app 컨테이너 중지 및 삭제"
APP_IMAGE_IDS="$(compose images -q app 2>/dev/null || true)"
compose stop app 2>/dev/null || true
compose rm -f app 2>/dev/null || true

echo "==> [5/7] 기존 app 이미지 삭제"
while IFS= read -r image_id; do
  [[ -n "$image_id" ]] && docker image rm "$image_id" || true
done <<< "$APP_IMAGE_IDS"

echo "==> [6/7] app 이미지 빌드"
compose build app

echo "==> [7/7] app 기동"
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
