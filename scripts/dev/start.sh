#!/usr/bin/env bash
# 启动开发环境：基础设施容器 + 后端 + 前端
set -euo pipefail
cd "$(dirname "$0")/../.."

docker compose -f deployment/compose/docker-compose.dev.yml up -d
echo "[dev] waiting for mysql..."
for i in $(seq 1 30); do
  if docker exec release-mysql mysqladmin ping -h localhost --silent; then break; fi
  sleep 2
done

(cd backend && ./mvnw spring-boot:run) &
BACK_PID=$!

if [ -d frontend/node_modules ]; then
  (cd frontend && pnpm dev) &
  FE_PID=$!
else
  echo "[dev] frontend deps not installed; skip pnpm dev (run scripts/build/frontend.sh first)"
  FE_PID=""
fi

trap 'kill $BACK_PID ${FE_PID:+$FE_PID} 2>/dev/null || true' EXIT
wait
