#!/bin/bash
# 轻舟云 MySQL 容灾脚本集
# 主从: zy-mysql(:3306 主) <-> zy-mysql-replica(:3307 从)
# 用法: ./mysql-dr.sh backup|status|check|switch|restore
set -e
MYSQL="docker exec zy-mysql mysql -uroot -pzy@2026"
REPLICA="docker exec zy-mysql-replica mysql -uroot -pzy@2026"
BK_DIR="${BK_DIR:-./backups}"
DATE=$(date +%Y%m%d_%H%M%S)

case "${1:-status}" in
  backup)
    mkdir -p "$BK_DIR"
    echo "[1/2] 全量备份(主库, 单事务一致)"
    docker exec zy-mysql mysqldump -uroot -pzy@2026 \
      --single-transaction --master-data=2 --all-databases --routines --triggers \
      > "$BK_DIR/zy-master-$DATE.sql"
    echo "       -> $BK_DIR/zy-master-$DATE.sql ($(du -h "$BK_DIR/zy-master-$DATE.sql" | cut -f1))"
    echo "[2/2] 记录位点"
    $MYSQL -N -e "SHOW MASTER STATUS;" | awk '{print $1, $2}' > "$BK_DIR/zy-master-$DATE.pos"
    cat "$BK_DIR/zy-master-$DATE.pos"
    echo "备份完成: $BK_DIR/zy-master-$DATE.sql"
    ;;
  status)
    echo "=== 主库 ==="
    $MYSQL -e "SHOW MASTER STATUS\G" | grep -E "File|Position"
    echo "=== 从库 ==="
    $REPLICA -e "SHOW SLAVE STATUS\G" | grep -E "Slave_IO_Running|Slave_SQL_Running|Seconds_Behind_Master"
    ;;
  check)
    echo "=== 复制一致性抽查 ==="
    MASTER=$(docker exec zy-mysql mysql -uroot -pzy@2026 -N qingzhouyun -e "SELECT COUNT(*) FROM zy_user;" 2>/dev/null)
    REPL=$(docker exec zy-mysql-replica mysql -uroot -pzy@2026 -N qingzhouyun -e "SELECT COUNT(*) FROM zy_user;" 2>/dev/null)
    echo "主库 zy_user: $MASTER  从库 zy_user: $REPL"
    [ "$MASTER" = "$REPL" ] && echo "一致性 OK" || echo "一致性异常!"
    ;;
  switch)
    # 故障切换: 提升从库(需先停主库业务, 演示脚本)
    echo "=== 故障切换: 提升从库为可写 ==="
    $REPLICA -e "STOP SLAVE; RESET SLAVE ALL;" 
    $REPLICA -e "SET GLOBAL read_only=OFF;"
    echo "从库已提升为可写(应用切换 DB_HOST 至 3307)"
    ;;
  restore)
    # 恢复: 全量恢复主库(危险操作, 需确认)
    [ -z "$2" ] && echo "用法: $0 restore <备份文件>" && exit 1
    echo "警告: 将覆盖主库全部数据! 5 秒后执行 (Ctrl-C 取消)"
    sleep 5
    docker exec -i zy-mysql mysql -uroot -pzy@2026 < "$2"
    echo "恢复完成"
    ;;
  *)
    echo "用法: $0 {backup|status|check|switch|restore <file>}"
    ;;
esac
