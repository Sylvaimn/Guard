package com.temporedata.guard.modules.dev.ingestion.service;

import com.temporedata.guard.modules.common.lakehouse.service.LakehouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CDC 实时采集消费者: 监听 tdg-cdc topic(binlog 变更事件), 写入 Iceberg 湖仓。
 * 事件格式: {db, table, op(INSERT/UPDATE/DELETE), ts, rows:[{列:值}]}
 * 当前支持轨迹聚合表(track_flow_daily)入湖; 其他表按通用策略追加。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CdcIngestionListener {

    private final com.temporedata.guard.modules.common.lakehouse.service.LakehouseService lakehouseService;
    private final IngestionOffsetService offsetService;

    @KafkaListener(topics = "tdg-cdc", groupId = "tdg-cdc")
    public void onCdcEvent(Map<String, Object> event) {
        String table = String.valueOf(event.getOrDefault("table", ""));
        String op = String.valueOf(event.getOrDefault("op", "INSERT"));
        try {
            Object rowsObj = event.get("rows");
            if (rowsObj == null) {
                log.warn("[CDC] event without rows, skipped");
                return;
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) rowsObj;

            if ("track_flow_daily".equals(table)) {
                List<Map<String, Object>> lakeRows = new ArrayList<>();
                for (Map<String, Object> r : rows) {
                    if (r.containsKey("region") && r.containsKey("stat_date")) {
                        Map<String, Object> m = new java.util.HashMap<>();
                        m.put("region", r.get("region"));
                        m.put("stat_date", String.valueOf(r.get("stat_date")).length() > 10
                                ? String.valueOf(r.get("stat_date")).substring(0, 10) : String.valueOf(r.get("stat_date")));
                        m.put("renci", r.get("renci") == null ? 0 : ((Number) r.get("renci")).intValue());
                        m.put("guiji", r.get("guiji") == null ? 0 : ((Number) r.get("guiji")).longValue());
                        lakeRows.add(m);
                    }
                }
                if (!lakeRows.isEmpty()) {
                    lakehouseService.createTable(LakehouseService.TABLE);
                    lakehouseService.append(LakehouseService.TABLE, lakeRows);
                    log.info("[CDC] {} {} -> lake, {} rows", op, table, lakeRows.size());
                }
            } else {
                log.info("[CDC] table {} op {} received ({} rows), 暂未映射湖仓表", table, op,
                        rows == null ? 0 : rows.size());
            }
        } catch (Exception e) {
            log.warn("[CDC] ingestion failed: {}", e.getMessage());
        } finally {
            try {
                offsetService.saveOffset("tdg-cdc:" + table + ":" + op, String.valueOf(event.getOrDefault("ts", "")));
            } catch (Exception e) {
                log.warn("[CDC] saveOffset failed: {}", e.getMessage());
            }
        }
    }
}
