#!/bin/bash
# 轻舟云 数据治理脚本: 分区归档 + 轻量湖仓导出
# ① 归档: 业务库历史分区/旧数据迁移到归档库 zy_archive
# ② 湖仓: 分析结果导出到湖仓目录(目录规范 + 元数据表)
set -e
MYSQL="docker exec zy-mysql mysql -uroot -pzy@2026"
LAKE_DIR="${LAKE_DIR:-./data/lake}"
DATE=$(date +%Y%m%d)
MONTH_BEFORE=$(date -v-2m +%Y%m 2>/dev/null || date -d '-2 months' +%Y%m)

case "${1:-help}" in
  archive)
    echo "[归档] 创建归档库与归档表(以 ${MONTH_BEFORE} 前数据为例)"
    $MYSQL -e "CREATE DATABASE IF NOT EXISTS zy_archive DEFAULT CHARACTER SET utf8mb4;"
    $MYSQL zy_archive -e "
      CREATE TABLE IF NOT EXISTS track_point_archived LIKE zy_analysis.track_point_partitioned;
      INSERT INTO track_point_archived
        SELECT * FROM zy_analysis.track_point_partitioned WHERE point_time < DATE_FORMAT(DATE_SUB(NOW(), INTERVAL 60 DAY), '%Y-%m-01');
      SELECT CONCAT('归档行数: ', ROW_COUNT()) AS result;"
    echo "归档完成 -> zy_archive(历史数据与业务库分离, 查询走归档库)"
    ;;
  lake-export)
    echo "[湖仓] 导出分析结果到湖仓目录"
    mkdir -p "$LAKE_DIR/track_flow_daily/dt=$DATE"
    $MYSQL -N zy_analysis -e "SELECT region, person_count, track_count FROM track_flow_daily WHERE stat_date >= '2026-08-18';" \
      | awk '{print $1", "$2", "$3}' > "$LAKE_DIR/track_flow_daily/dt=$DATE/part.csv"
    # 元数据表
    ROWS=$(wc -l < "$LAKE_DIR/track_flow_daily/dt=$DATE/part.csv")
    SIZE=$(du -k "$LAKE_DIR/track_flow_daily/dt=$DATE/part.csv" | cut -f1)
    $MYSQL zy_analysis -e "
      CREATE TABLE IF NOT EXISTS zy_lake_meta (
        table_name VARCHAR(64), partition_path VARCHAR(255), row_count INT, size_kb INT, export_time DATETIME
      );
      INSERT INTO zy_lake_meta VALUES ('track_flow_daily', 'dt=$DATE', $ROWS, $SIZE, NOW());"
    echo "湖仓导出: $LAKE_DIR/track_flow_daily/dt=$DATE/part.csv ($ROWS 行, ${SIZE}KB)"
    echo "元数据已登记 zy_lake_meta"
    ;;
  lake-meta)
    echo "[湖仓元数据]"
    $MYSQL zy_analysis -e "SELECT table_name, partition_path, row_count, size_kb, export_time FROM zy_lake_meta ORDER BY export_time DESC;" 2>/dev/null | tail -5
    ;;
  *)
    echo "用法: $0 {archive|lake-export|lake-meta}"
    ;;
esac
