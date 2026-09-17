#!/usr/bin/env bash
# 轻舟云 qingzhouyun - one-command build producing a single deployable jar.
# Result: dist/qingzhouyun.jar (backend + bundled frontend SPA).
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

GRADLE_BIN="${GRADLE_BIN:-gradle}"
STATIC_DIR="temporedata-guard-main/src/main/resources/static"

echo "==> [1/3] Building frontend (Vue3)..."
(cd zy-frontend && npm install --no-audit --no-fund && npm run build)
echo "    frontend bundled into ${STATIC_DIR}"

echo "==> [2/3] Building backend fat jar..."
"$GRADLE_BIN" :zy-backend:zy-main:bootJar --no-daemon

echo "==> [3/3] Assembling dist/qingzhouyun.jar..."
mkdir -p dist
cp zy-backend/zy-main/build/libs/qingzhouyun.jar dist/qingzhouyun.jar

echo ""
echo "==> Done. Run with:  java -jar dist/qingzhouyun.jar"
echo "    Web UI:  http://localhost:8080     (default login: admin / admin123)"