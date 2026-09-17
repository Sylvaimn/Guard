package com.temporedata.guard.modules.dev.quality.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 实时质量规则: 监听 CDC 流(tdg-cdc), 对入库前数据做流式校验(字段完整性/数值范围),
 * 校验结果写入 temporedata_guard_quality_check(实时类型), 异常事件告警日志。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeQualityListener {

    private final JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "tdg-cdc", groupId = "tdg-quality")
    public void onCdcEvent(Map<String, Object> event) {
        try {
            String table = String.valueOf(event.getOrDefault("table", ""));
            String op = String.valueOf(event.getOrDefault("op", "INSERT"));
            Object rowsObj = event.get("rows");
            if (rowsObj == null) return;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rows = (List<Map<String, Object>>) rowsObj;

            long start = System.currentTimeMillis();
            int total = rows == null ? 0 : rows.size();
            int failed = 0;
            StringBuilder err = new StringBuilder();

            for (Map<String, Object> r : rows) {
                String issue = validateRow(table, r);
                if (issue != null) {
                    failed++;
                    if (err.length() < 300) err.append(issue).append("; ");
                }
            }

            String status = failed == 0 ? "REALTIME_PASS" : "REALTIME_FAIL";
            String result = String.format("{\"table\":\"%s\",\"op\":\"%s\",\"total\":%d,\"failed\":%d}", table, op, total, failed);
            jdbcTemplate.update(
                    "INSERT INTO temporedata_guard_quality_check (id, rule_id, status, result, duration_ms, error_msg, check_time) VALUES (?,?,?,?,?,?,?)",
                    UUID.randomUUID().toString().replace("-", ""),
                    "REALTIME:" + table, status, result, System.currentTimeMillis() - start,
                    err.length() == 0 ? null : err.toString(), LocalDateTime.now());

            if (failed > 0) {
                log.warn("[REALTIME-QUALITY] {} {}: {} rows, {} failed -> {}", table, op, total, failed, err);
            } else {
                log.info("[REALTIME-QUALITY] {} {}: {} rows PASS", table, op, total);
            }
        } catch (Exception e) {
            log.warn("[REALTIME-QUALITY] check failed: {}", e.getMessage());
        }
    }

    /** 行级校验规则: 轨迹聚合表字段完整性/数值范围 */
    private String validateRow(String table, Map<String, Object> r) {
        if ("track_flow_daily".equals(table)) {
            if (r.get("region") == null || String.valueOf(r.get("region")).isBlank()) return "region 为空";
            if (r.get("stat_date") == null) return "stat_date 为空";
            int renci = r.get("renci") == null ? 0 : ((Number) r.get("renci")).intValue();
            long guiji = r.get("guiji") == null ? 0 : ((Number) r.get("guiji")).longValue();
            if (renci < 0) return "renci 为负(" + renci + ")";
            if (guiji < 0) return "guiji 为负(" + guiji + ")";
            if (renci > 100000) return "renci 超阈值(" + renci + ")";
        }
        return null;
    }
}
