#!/usr/bin/env bash
# ============================================================
# TemporeData Guard · 包迁移工具（复用/审计）
# 功能: 目录移动 + package/import 全仓库替换(旧包→新包)
# 用法:
#   ./migrate-package.sh apply OLD_PKG NEW_PKG   # 执行迁移(目录+内容)
#   ./migrate-package.sh audit OLD_PKG           # 审计残留(源码/配置)
# 示例: ./migrate-package.sh apply com.isxcode.zy com.temporedata.guard
#       ./migrate-package.sh audit com.isxcode.zy
# ============================================================
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"

apply() {
  local OLD="$1" NEW="$2"
  local OLD_PATH="${OLD//.//}" NEW_PATH="${NEW//.//}"
  echo "[1/3] Java 目录移动: $OLD_PATH → $NEW_PATH"
  local moved=0
  while IFS= read -r -d '' p; do
    local newp="${p/$OLD_PATH/$NEW_PATH}"
    mkdir -p "$(dirname "$newp")"
    mv "$p" "$newp"
    moved=$((moved+1))
  done < <(find temporedata-guard-* -path "*$OLD_PATH*" -name "*.java" -print0 2>/dev/null)
  echo "      移动 $moved 个文件"
  # 清理空目录
  find . -type d -name "$(basename "$OLD_PATH")" -empty -delete 2>/dev/null

  echo "[2/3] package/import 内容替换(源码)"
  local cnt=0
  while IFS= read -r -d '' p; do
    if grep -q "$OLD" "$p" 2>/dev/null; then
      python3 - "$p" "$OLD" "$NEW" << 'PY'
import sys
p, o, n = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(p, encoding='utf-8').read()
open(p, 'w', encoding='utf-8').write(s.replace(o, n))
PY
      cnt=$((cnt+1))
    fi
  done < <(find temporedata-guard-* -name "*.java" -print0 2>/dev/null)
  echo "      替换 $cnt 个 Java 文件"

  echo "[3/3] 配置/资源替换(yml/xml/properties/sql)"
  local cnt2=0
  while IFS= read -r -d '' p; do
    if grep -q "$OLD" "$p" 2>/dev/null; then
      python3 - "$p" "$OLD" "$NEW" << 'PY'
import sys
p, o, n = sys.argv[1], sys.argv[2], sys.argv[3]
s = open(p, encoding='utf-8').read()
open(p, 'w', encoding='utf-8').write(s.replace(o, n))
PY
      cnt2=$((cnt2+1))
    fi
  done < <(find temporedata-guard-* \( -name "*.yml" -o -name "*.xml" -o -name "*.properties" -o -name "*.sql" \) -print0 2>/dev/null)
  echo "      替换 $cnt2 个配置文件"
  echo "[DONE] 迁移完成, 执行审计: $0 audit $OLD"
}

audit() {
  local OLD="$1"
  echo "=== 源码残留审计(排除 target) ==="
  grep -rl "$OLD" --include="*.java" --include="*.vue" --include="*.js" --include="*.yml" --include="*.xml" . 2>/dev/null \
    | grep -v node_modules | grep -v "/target/" | head -10
  echo "=== 目录残留 ==="
  find . -path "*${OLD//.//}*" -not -path "*/target/*" 2>/dev/null | head -5
  echo "(以上为空 = 干净)"
}

case "${1:-}" in
  apply) [ $# -ge 3 ] && apply "$2" "$3" || echo "用法: $0 apply OLD_PKG NEW_PKG" ;;
  audit) [ $# -ge 2 ] && audit "$2" || echo "用法: $0 audit OLD_PKG" ;;
  *) echo "用法: $0 {apply OLD NEW | audit OLD}" ;;
esac
