package com.temporedata.guard.modules.dev.ingestion.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 采集断点续传: 记录 CDC/采集任务的消费位点, 失败后从上次位点恢复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionOffsetService {

    private final JdbcTemplate jdbcTemplate;

    /** 保存位点 */
    public void saveOffset(String sourceKey, String offsetValue) {
        jdbcTemplate.update(
                "INSERT INTO temporedata_guard_ingestion_offset (source_key, offset_value, last_event_time, update_time) VALUES (?,?,?,NOW()) " +
                        "ON DUPLICATE KEY UPDATE offset_value = VALUES(offset_value), last_event_time = VALUES(last_event_time), update_time = NOW()",
                sourceKey, offsetValue, LocalDateTime.now());
    }

    /** 读取位点(续传) */
    public String getOffset(String sourceKey) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT offset_value FROM temporedata_guard_ingestion_offset WHERE source_key = ?", String.class, sourceKey);
        } catch (Exception e) {
            return null;
        }
    }

    /** 位点状态(监控) */
    public java.util.List<java.util.Map<String, Object>> offsets() {
        return jdbcTemplate.queryForList(
                "SELECT source_key, offset_value, last_event_time, update_time FROM temporedata_guard_ingestion_offset ORDER BY update_time DESC");
    }

    /** 源库行数统计(对账): 优先分析库(temporedata_guard_analysis) */
    public int countSourceRows(String table) {
        try {
            Integer n = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM temporedata_guard_analysis." + table, Integer.class);
            return n == null ? 0 : n;
        } catch (Exception e) {
            try {
                Integer n = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table, Integer.class);
                return n == null ? 0 : n;
            } catch (Exception e2) {
                throw new com.temporedata.guard.api.base.exceptions.BusinessException("源表统计失败: " + e2.getMessage());
            }
        }
    }
}
