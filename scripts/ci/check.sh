#!/usr/bin/env bash
# CI 质量检查入口：后端测试 + 前端构建与测试
set -euo pipefail
cd "$(dirname "$0")/../.."

echo "== backend: mvn verify =="
(cd backend && mvn -B verify)

echo "== frontend: pnpm install + build + test =="
if [ -d frontend ]; then
  (cd frontend && pnpm install --frozen-lockfile=false && pnpm build && pnpm test)
fi

echo "== CI check passed =="
