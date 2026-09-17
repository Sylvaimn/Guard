package com.temporedata.guard.modules.dev.meta.service;

import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.plugin.PluginRegistry;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 统一元数据目录: 聚合各数据源插件的元数据(表/topic/湖仓表)到 temporedata_guard_meta_unified,
 * 提供跨 MySQL/ClickHouse/Iceberg/Kafka 的统一视图。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaUnifiedService {

    private final DatasourceRepository datasourceRepository;
    private final PluginRegistry pluginRegistry;
    private final JdbcTemplate jdbcTemplate;

    /** 全量同步: 遍历数据源, 调插件 meta, 写入统一目录 */
    @Transactional
    public int syncAll(String tenantId) {
        jdbcTemplate.update("DELETE FROM temporedata_guard_meta_unified WHERE tenant_id = ?", tenantId);
        int total = 0;
        for (DatasourceEntity ds : datasourceRepository.findAll()) {
            try {
                List<Map<String, Object>> items = pluginRegistry.get(ds.getType()).meta(ds);
                for (Map<String, Object> it : items) {
                    String name = String.valueOf(it.getOrDefault("name", ""));
                    if (name.isEmpty() || "null".equals(name)) continue;
                    String type = String.valueOf(it.getOrDefault("type", "TABLE"));
                    jdbcTemplate.update(
                            "INSERT INTO temporedata_guard_meta_unified (id, datasource_id, datasource_name, source_type, item_name, item_type, extra, tenant_id, sync_time) VALUES (?,?,?,?,?,?,?,?,?)",
                            UUID.randomUUID().toString().replace("-", ""),
                            ds.getId(), ds.getName(), ds.getType(), name, type,
                            String.valueOf(it.getOrDefault("snapshotId", it.getOrDefault("internal", ""))),
                            tenantId, LocalDateTime.now());
                    total++;
                }
            } catch (Exception e) {
                log.warn("meta sync skip datasource {}: {}", ds.getName(), e.getMessage());
            }
        }
        return total;
    }

    /** 统一目录查询 */
    public List<Map<String, Object>> list(String tenantId, String sourceType) {
        if (sourceType != null && !sourceType.isBlank()) {
            return jdbcTemplate.queryForList(
                    "SELECT datasource_name, source_type, item_name, item_type, extra, sync_time FROM temporedata_guard_meta_unified WHERE tenant_id = ? AND source_type = ? ORDER BY source_type, item_name",
                    tenantId, sourceType);
        }
        return jdbcTemplate.queryForList(
                "SELECT datasource_name, source_type, item_name, item_type, extra, sync_time FROM temporedata_guard_meta_unified WHERE tenant_id = ? ORDER BY source_type, item_name",
                tenantId);
    }
}
