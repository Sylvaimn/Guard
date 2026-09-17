#!/usr/bin/env bash
# ============================================================
# TemporeData Guard · 一键自检(构建+启动+完整回归44+清理)
# 适用: 普通终端(非沙箱)单命令完成验收
#   ./verify-all.sh            # 全流程(构建+回归44)
#   ./verify-all.sh --no-build # 跳过构建(已构建时加速)
#   ./verify-all.sh --only-reg # 仅回归(服务已在跑)
# ============================================================
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
LOG_DIR="${TMPDIR:-/tmp}"
PORT="${SERVER_PORT:-8080}"

echo "========== TemporeData Guard 一键自检 =========="
echo "[0] 环境预检: MySQL/湖仓/jar"
nc -z localhost 3306 >/dev/null 2>&1 || { echo "[ERR] MySQL(:3306) 不可达, 请先启动容器"; exit 1; }
[ -d data/lake-iceberg ] || mkdir -p data/lake-iceberg
[ -f temporedata-guard-main/target/tdg.jar ] || { echo "[WARN] tdg.jar 缺失 → 执行构建"; mvn -T 1C clean install -DskipTests >/dev/null 2>&1 || { echo "[ERR] 后端构建失败"; exit 1; }; (cd temporedata-guard-ui && npm run build >/dev/null 2>&1 || true); }

if [ "${1:-}" != "--only-reg" ]; then
  echo "[1] 构建(如需): mvn install + npm build"
  [ "${1:-}" = "--no-build" ] || { mvn -T 1C clean install -DskipTests 2>&1 | tail -1; (cd temporedata-guard-ui && npm run build >/dev/null 2>&1 || true); }
fi

echo "[2] 清理旧进程/端口"
pkill -f "tdg.jar" 2>/dev/null; lsof -iTCP:$PORT -sTCP:LISTEN 2>/dev/null | awk 'NR>1{print $2}' | xargs kill -9 2>/dev/null; sleep 2

echo "[3] 启动主应用 :$PORT (等待健康, 约1-3分钟)"
nohup env SPRING_PROFILES_ACTIVE=prod DB_HOST=localhost DB_PORT=3306 DB_NAME=temporedata \
  DB_USERNAME=root DB_PASSWORD=zy@2026 NACOS_ENABLED=false EVENT_BUS_TYPE=kafka \
  KAFKA_SERVERS=localhost:9094 SERVER_PORT=$PORT \
  java -Xmx512m -Xms256m -jar temporedata-guard-main/target/tdg.jar \
  --app.lineage.enabled=true --app.audit.listener.enabled=true \
  --lakehouse.warehouse="$ROOT/data/lake-iceberg" > "$LOG_DIR/verify-main.log" 2>&1 &
disown
ok=0
for i in $(seq 1 60); do
  grep -q "Started ZyApplication" "$LOG_DIR/verify-main.log" 2>/dev/null && { ok=1; echo "    主应用 Started @ $((i*5))s"; break; }
  sleep 5
done
[ "$ok" != "1" ] && { echo "[ERR] 主应用未启动, 查看 $LOG_DIR/verify-main.log"; exit 1; }

echo "[4] 完整回归 44 项(约2-4分钟)"
sleep 3
RES=$(bash scripts/police-combat-test.sh 2>&1 | sed 's/\x1b\[[0-9;]*m//g' | grep -cE '^\[OK\]')
echo "========== 回归结果: $RES / 44 =========="
if [ "$RES" = "44" ]; then
  echo "✅ 全部通过 (TemporeData Guard 验收成功)"
else
  echo "⚠️ 未达 44, 查看上述 [FAIL] 项或日志 $LOG_DIR/verify-main.log"
  exit 1
fi

echo "[5] 清理(可选): pkill -f tdg.jar"
