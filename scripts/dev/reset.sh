#!/usr/bin/env bash
# 重置开发数据（删除卷，危险操作）
set -euo pipefail
read -p "This will DROP all dev data (mysql/redis volumes). Continue? [y/N] " ans
[[ "${ans:-N}" == "y" || "${ans:-N}" == "Y" ]] || { echo "aborted"; exit 1; }
cd "$(dirname "$0")/../.."
docker compose -f deployment/compose/docker-compose.dev.yml down -v
echo "[dev] reset done."
