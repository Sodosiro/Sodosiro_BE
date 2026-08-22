#!/usr/bin/env bash
# postgres docker-entrypoint-initdb.d 에서 첫 기동 시 1회 실행됩니다.
# pgvector 활성화 → DDL 적용 → CSV 시드 적재
set -euo pipefail

APP_DB="${POSTGRES_DB:-sodosiro}"
SQL_DIR="/docker/postgres"
CSV_DIR="$SQL_DIR/seed/csv"

echo "==> [1/3] pgvector 확장 활성화"
psql -v ON_ERROR_STOP=1 -d "$APP_DB" -c "CREATE EXTENSION IF NOT EXISTS vector;"

echo "==> [2/3] DDL 적용"
psql -v ON_ERROR_STOP=1 -d "$APP_DB" -f "$SQL_DIR/000_initialize_travel_schema.sql"
psql -v ON_ERROR_STOP=1 -d "$APP_DB" -f "$SQL_DIR/001_add_notification_schema.sql"

echo "==> [3/3] CSV 시드 데이터"

if ! compgen -G "$CSV_DIR/*.csv" > /dev/null 2>&1; then
  echo "  CSV 없음 ($CSV_DIR) — 앱은 빈 DB로 시작합니다."
  exit 0
fi

TABLES_IN_ORDER=(
  area_code
  category
  sigungu_code
  tourist_spot
  spot_image
  spot_popularity
  spot_ai_recommendation
  etl_spot_state
  spot_embedding
)

# CSV 헤더를 읽어 컬럼 순서를 자동 지정 (테이블 정의 순서와 달라도 안전)
{
  echo "SET session_replication_role = replica;"
  for table in "${TABLES_IN_ORDER[@]}"; do
    csv="$CSV_DIR/$table.csv"
    [[ -f "$csv" ]] || continue
    # CSV가 CRLF일 수 있으므로 마지막 헤더 컬럼에 붙는 \r을 제거한다.
    cols=$(head -n 1 "$csv" | tr -d '\r"' | awk -F, '{for(i=1;i<=NF;i++) printf "\"%s\"%s",$i,(i<NF?",":"")}')
    # POSIX sh의 echo는 \c/\e를 제어 문자로 해석할 수 있으므로 psql 메타 명령은 printf로 전달한다.
    printf '%s\n' "\copy $table ($cols) FROM '$csv' CSV HEADER"
    printf '%s\n' "\echo '  [LOAD] $table'"
  done
  echo "SET session_replication_role = DEFAULT;"
} | psql -v ON_ERROR_STOP=1 -d "$APP_DB"

psql -d "$APP_DB" << 'SQL'
SELECT setval(pg_get_serial_sequence('sigungu_code', 'id'), COALESCE((SELECT MAX(id) FROM sigungu_code), 1));
SELECT setval(pg_get_serial_sequence('spot_image',   'id'), COALESCE((SELECT MAX(id) FROM spot_image),   1));
SQL

count="$(psql -d "$APP_DB" -tAc "SELECT COUNT(*) FROM tourist_spot")"
echo "  완료: tourist_spot ${count//[[:space:]]/}건 적재"
