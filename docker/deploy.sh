#!/usr/bin/env bash
# app 이미지만 다시 빌드해서 재배포한다. postgres/redis 는 건드리지 않는다.
# 사용법: ./docker/deploy.sh   (프로젝트 루트 어디서 실행해도 됨)
set -euo pipefail

# 스크립트 위치 기준으로 프로젝트 루트로 이동
cd "$(dirname "$0")/.."

COMPOSE="docker compose -f docker/docker-compose.yml"

echo "==> [1/4] 인프라 확인/기동 (postgres, redis)"
$COMPOSE up -d postgres redis

echo "==> [2/4] app 이미지 빌드"
$COMPOSE build app

echo "==> [3/4] app 재배포 (--no-deps: 인프라 재시작 안 함)"
$COMPOSE up -d --no-deps app

echo "==> [4/4] 상태 & 최근 로그"
$COMPOSE ps
echo "----- app logs (tail) -----"
$COMPOSE logs --tail=40 app

echo
echo "완료. 실시간 로그: $COMPOSE logs -f app"
echo "Swagger UI: http://localhost:8080/swagger-ui/index.html"
