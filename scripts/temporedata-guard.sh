#!/usr/bin/env bash
# ============================================================
# 金盾智云警用大数据中台 · 统一运行管理脚本（本机 :8080）
# 对应《技术方案设计文档(业务需求驱动 v2.2)》模式一/模式二
# 用法:
#   ./qingzhouyun.sh start            # 启动主应用 :8080(等待健康)
#   ./qingzhouyun.sh start services   # 启动 6 拆分服务 :8090-8095
#   ./qingzhouyun.sh stop             # 停止主应用
#   ./qingzhouyun.sh stop services    # 停止拆分服务
#   ./qingzhouyun.sh status           # 端口/进程/健康一览
#   ./qingzhouyun.sh logs [svc]       # 跟踪日志(main|platform|security|data|police|agent|dev)
#   ./qingzhouyun.sh dev              # 前端热更新提示(npm run dev :5174)
# ============================================================
set -u
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="${TMPDIR:-/var/folders/yt/331j9bm925j4qs7b64r_j1200000gn/T/opencode}"
mkdir -p "$LOG_DIR"

# 统一运行参数(模式一/二, 与设计文档 4.2 对齐)
DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-3306}"
DB_NAME="${DB_NAME:-temporedata}"
DB_USER="${DB_USERNAME:-root}"
DB_PASS="${DB_PASSWORD:-zy@2026}"
SERVER_PORT="${SERVER_PORT:-8080}"
WAREHOUSE="${LAKEHOUSE_WAREHOUSE:-$ROOT/data/lake-iceberg}"
XMX="${MAIN_XMX:-512m}"
XMS="${MAIN_XMS:-256m}"
SVC_XMX="192m"
SVC_XMS="96m"

MAIN_JAR="$ROOT/temporedata-guard-main/target/tdg.jar"
SVC_LIST="platform:8091 security:8090 data:8092 police:8093 agent:8094 dev:8095"
port_of() { for x in $SVC_LIST; do [ "${x%%:*}" = "$1" ] && { echo "${x##*:}"; return; }; done; }

health() { # $1=port
  local code
  code=$(curl -s -m 2 -o /dev/null -w "%{http_code}" "http://localhost:$1/" 2>/dev/null)
  echo "$code"
}

start_main() {
  [ -f "$MAIN_JAR" ] || { echo "[ERR] 未找到主应用 jar: $MAIN_JAR（请先 mvn package）"; exit 1; }
  pgrep -f "tdg.jar" >/dev/null && { echo "[SKIP] 主应用已在运行(:$SERVER_PORT)"; return 0; }
  [ -d "$(dirname "$WAREHOUSE")" ] || mkdir -p "$(dirname "$WAREHOUSE")"
  echo "[START] 主应用 :$SERVER_PORT  (湖仓: $WAREHOUSE)"
  nohup env SPRING_PROFILES_ACTIVE=prod DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" \
    DB_USERNAME="$DB_USER" DB_PASSWORD="$DB_PASS" NACOS_ENABLED=false \
    EVENT_BUS_TYPE=kafka KAFKA_SERVERS="localhost:9094" SERVER_PORT="$SERVER_PORT" \
    java -Xmx"$XMX" -Xms"$XMS" -jar "$MAIN_JAR" \
    --app.lineage.enabled=true --app.audit.listener.enabled=true \
    --lakehouse.warehouse="$WAREHOUSE" > "$LOG_DIR/qingzhouyun-main.log" 2>&1 &
  disown
  echo -n "[WAIT] 等待健康"
  for i in $(seq 1 90); do
    sleep 3
    if [ "$(health "$SERVER_PORT")" = "200" ]; then echo " OK(约$((i*3))s)"; return 0; fi
    echo -n "."
  done
  echo " 超时! 查看: $LOG_DIR/qingzhouyun-main.log"
}

start_services() {
  for x in $SVC_LIST; do
    local name="${x%%:*}" port="${x##*:}" jar="$ROOT/temporedata-guard-service-${x%%:*}/target/tdg-service-${x%%:*}.jar"
    [ -f "$jar" ] || { echo "[WARN] $name jar 缺失: $jar"; continue; }
    pgrep -f "tdg-service-$name.jar" >/dev/null && { echo "[SKIP] $name 已在运行(:$port)"; continue; }
    nohup env SPRING_PROFILES_ACTIVE=prod DB_HOST="$DB_HOST" DB_PORT="$DB_PORT" DB_NAME="$DB_NAME" \
      DB_USERNAME="$DB_USER" DB_PASSWORD="$DB_PASS" NACOS_ENABLED=false \
      EVENT_BUS_TYPE=kafka KAFKA_SERVERS="localhost:9094" SERVER_PORT="$port" \
      java -Xmx"$SVC_XMX" -Xms"$SVC_XMS" -jar "$jar" > "$LOG_DIR/qingzhouyun-$name.log" 2>&1 &
    disown
    echo "[START] $name :$port"
  done
}

stop_main() {
  pkill -f "tdg.jar" && echo "[STOP] 主应用已停止" || echo "[INFO] 主应用未运行"
}
stop_services() {
  for x in $SVC_LIST; do
    pkill -f "tdg-service-${x%%:*}.jar" >/dev/null 2>&1 && echo "[STOP] ${x%%:*}"
  done
}

status() {
  echo "=== 端口/进程/健康 ==="
  for p in 8080 8090 8091 8092 8093 8094 8095; do
    local h
    h=$(health "$p")
    printf "  :%-5s %-5s\n" "$p" "$h"
  done
  echo "=== 进程 ==="
  pgrep -fl "java.*qingzhouyun.*jar" | grep -oE "qingzhouyun[-.]?[a-z]*\.jar" | sort -u | tr '\n' ' '; echo
}

case "${1:-}" in
  start) start_main; shift; [ "${1:-}" = "services" ] && start_services ;;
  stop) stop_main; shift; [ "${1:-}" = "services" ] && stop_services ;;
  status) status ;;
  logs) tail -f "$LOG_DIR/qingzhouyun-${2:-main}.log" ;;
  dev) echo "前端热更新(模式一): cd temporedata-guard-ui && npm run dev  → http://localhost:5174  (/api 已代理到 :$SERVER_PORT)";;
  *) echo "用法: $0 {start [services]|stop [services]|status|logs [svc]|dev}"; exit 1 ;;
esac
