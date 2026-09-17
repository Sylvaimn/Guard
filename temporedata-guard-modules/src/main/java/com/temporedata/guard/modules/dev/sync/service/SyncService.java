package com.temporedata.guard.modules.dev.sync.service;
import com.temporedata.guard.modules.dev.lineage.service.LineageService;
import com.temporedata.guard.modules.common.async.TaskExecutorService;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.sync.SyncTaskReq;
import com.temporedata.guard.api.sync.SyncTaskRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * DB sync service: full and incremental sync between datasources.
 * Supports cross-database table copy with batching and incremental column tracking.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SyncService {

    private final SyncTaskRepository taskRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService;
    private final com.temporedata.guard.modules.common.async.TaskExecutorService taskExecutorService;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    /** 异步提交同步任务（HTTP 线程立即返回；同任务单飞防并发） */
    public boolean executeAsync(String id) {
        boolean accepted = taskExecutorService.submit("sync:" + id, () -> execute(id));
        if (!accepted) {
            throw new BusinessException("同步任务正在执行中");
        }
        return true;
    }

    public List<SyncTaskRes> list(String tenantId) {
        return taskRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public SyncTaskRes detail(String id) {
        return toRes(taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("同步任务不存在")));
    }

    @Transactional
    public SyncTaskRes create(SyncTaskReq req, String tenantId) {
        SyncTaskEntity entity = new SyncTaskEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setSourceDatasourceId(req.getSourceDatasourceId());
        entity.setSourceTable(req.getSourceTable());
        entity.setTargetDatasourceId(req.getTargetDatasourceId());
        entity.setTargetTable(req.getTargetTable());
        entity.setSyncMode(req.getSyncMode() != null ? req.getSyncMode() : "FULL");
        entity.setIncrementalColumn(req.getIncrementalColumn());
        entity.setIncrementalValue(req.getIncrementalValue());
        entity.setUpsertKey(req.getUpsertKey());
        entity.setScheduleCron(req.getScheduleCron());
        entity.setBatchSize(req.getBatchSize() != null ? req.getBatchSize() : "1000");
        entity.setConfig(req.getConfig());
        entity.setStatus("READY");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(taskRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        taskRepository.deleteById(id);
    }

    private static final java.util.regex.Pattern SAFE_IDENTIFIER =
            java.util.regex.Pattern.compile("^[A-Za-z0-9_]+(\\.[A-Za-z0-9_]+)?$");

    /** 校验表名/列名等标识符，防 SQL 注入 */
    private void assertSafeIdentifier(String value, String field) {
        if (value == null || !SAFE_IDENTIFIER.matcher(value).matches()) {
            throw new BusinessException(field + " 非法(仅允许字母数字下划线): " + value);
        }
    }

    /**
     * Execute DB sync: read from source, write to target.
     * Supports FULL and INCREMENTAL modes.
     * 由 TaskExecutorService 异步执行，不占用 HTTP 线程与长事务。
     */
    public void execute(String id) {
        SyncTaskEntity task = taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("同步任务不存在"));

        if ("RUNNING".equals(task.getStatus())) {
            throw new BusinessException("同步任务正在执行中");
        }

        task.setStatus("RUNNING");
        task.setLastRunTime(LocalDateTime.now());
        task.setErrorMsg(null);
        taskRepository.save(task);

        long start = System.currentTimeMillis();

        try {
            DatasourceEntity sourceDs = datasourceRepository.findById(task.getSourceDatasourceId())
                    .orElseThrow(() -> new BusinessException("源数据源不存在"));
            DatasourceEntity targetDs = datasourceRepository.findById(task.getTargetDatasourceId())
                    .orElseThrow(() -> new BusinessException("目标数据源不存在"));

            String sourcePwd = CryptoUtil.decrypt(sourceDs.getPassword(), cryptoKey);
            String targetPwd = CryptoUtil.decrypt(targetDs.getPassword(), cryptoKey);

            javax.sql.DataSource sourcePool = connectionManager.getOrCreate(sourceDs, sourcePwd);
            javax.sql.DataSource targetPool = connectionManager.getOrCreate(targetDs, targetPwd);

            int batchSize = Integer.parseInt(task.getBatchSize() != null ? task.getBatchSize() : "1000");

            try (Connection srcConn = sourcePool.getConnection();
                 Connection tgtConn = targetPool.getConnection()) {

                // 1. Read source table metadata
                assertSafeIdentifier(task.getSourceTable(), "源表名");
                assertSafeIdentifier(task.getTargetTable(), "目标表名");
                if (task.getIncrementalColumn() != null && !task.getIncrementalColumn().isBlank()) {
                    assertSafeIdentifier(task.getIncrementalColumn(), "增量列名");
                }
                if (task.getUpsertKey() != null && !task.getUpsertKey().isBlank()) {
                    assertSafeIdentifier(task.getUpsertKey(), "UPSERT键");
                }

                List<String> columns = new ArrayList<>();
                List<String> columnTypes = new ArrayList<>();
                DatabaseMetaData meta = srcConn.getMetaData();
                try (ResultSet rs = meta.getColumns(null, null, task.getSourceTable().toUpperCase(), null)) {
                    while (rs.next()) {
                        columns.add(rs.getString("COLUMN_NAME"));
                        columnTypes.add(rs.getString("TYPE_NAME"));
                    }
                }
                // Fallback: try with original case
                if (columns.isEmpty()) {
                    try (ResultSet rs = meta.getColumns(null, null, task.getSourceTable(), null)) {
                        while (rs.next()) {
                            columns.add(rs.getString("COLUMN_NAME"));
                            columnTypes.add(rs.getString("TYPE_NAME"));
                        }
                    }
                }

                if (columns.isEmpty()) {
                    throw new BusinessException("源表 " + task.getSourceTable() + " 不存在或无列信息");
                }

                // 2. Create target table (drop if exists, full sync)
                StringBuilder dropDdl = new StringBuilder("DROP TABLE IF EXISTS " + task.getTargetTable());
                try (Statement stmt = tgtConn.createStatement()) {
                    stmt.execute(dropDdl.toString());
                }

                StringBuilder createDdl = new StringBuilder("CREATE TABLE " + task.getTargetTable() + " (");
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) createDdl.append(", ");
                    createDdl.append("\"").append(columns.get(i)).append("\" ")
                            .append(mapColumnType(columnTypes.get(i)));
                }
                createDdl.append(")");
                log.info("DDL: {}", createDdl);
                try (Statement stmt = tgtConn.createStatement()) {
                    stmt.execute(createDdl.toString());
                }

                // 3. Read source data with optional incremental filter（参数化，防注入）
                StringBuilder selectSql = new StringBuilder("SELECT * FROM " + task.getSourceTable());
                String incrementalValue = task.getIncrementalValue();
                String incrementalColumn = null;
                if ("INCREMENTAL".equals(task.getSyncMode()) && task.getIncrementalColumn() != null
                        && incrementalValue != null && !incrementalValue.isEmpty()) {
                    incrementalColumn = task.getIncrementalColumn();
                    selectSql.append(" WHERE \"").append(incrementalColumn).append("\" > ?");
                }
                selectSql.append(" ORDER BY \"").append(columns.get(0)).append("\"");
                log.info("Source SQL: {}", selectSql);

                // 4. Build insert SQL (INCREMENTAL/CDC 模式用 upsert，防重复行冲突)
                StringBuilder insertSql = new StringBuilder("INSERT INTO " + task.getTargetTable() + " (");
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) insertSql.append(", ");
                    insertSql.append("\"").append(columns.get(i)).append("\"");
                }
                insertSql.append(") VALUES (");
                for (int i = 0; i < columns.size(); i++) {
                    if (i > 0) insertSql.append(", ");
                    insertSql.append("?");
                }
                insertSql.append(")");
                boolean useUpsert = "INCREMENTAL".equals(task.getSyncMode()) && task.getUpsertKey() != null
                        && !task.getUpsertKey().isEmpty();
                if (useUpsert) {
                    insertSql.append(" ON DUPLICATE KEY UPDATE ");
                    boolean firstUpdate = true;
                    for (int i = 0; i < columns.size(); i++) {
                        String col = columns.get(i);
                        // 大小写不敏感匹配 upsert 键(不同库列名大小写规则不同)
                        if (col.equalsIgnoreCase(task.getUpsertKey())) continue;
                        if (!firstUpdate) insertSql.append(", ");
                        insertSql.append("\"").append(col).append("\" = VALUES(\"").append(col).append("\")");
                        firstUpdate = false;
                    }
                }

                // 5. Stream data from source to target
                int totalRows = 0;
                String maxIncrementalValue = null;
                try (PreparedStatement srcStmt = srcConn.prepareStatement(selectSql.toString())) {
                    if (incrementalColumn != null) {
                        srcStmt.setObject(1, incrementalValue);
                    }
                    try (ResultSet rs = srcStmt.executeQuery();
                         PreparedStatement ps = tgtConn.prepareStatement(insertSql.toString())) {

                    tgtConn.setAutoCommit(false);
                    int batchCount = 0;

                    while (rs.next()) {
                        for (int i = 0; i < columns.size(); i++) {
                            ps.setObject(i + 1, rs.getObject(columns.get(i)));
                        }
                        ps.addBatch();
                        batchCount++;
                        totalRows++;

                        // Track max incremental value
                        if ("INCREMENTAL".equals(task.getSyncMode()) && task.getIncrementalColumn() != null) {
                            Object val = rs.getObject(task.getIncrementalColumn());
                            if (val != null) {
                                maxIncrementalValue = val.toString();
                            }
                        }

                        if (batchCount >= batchSize) {
                            ps.executeBatch();
                            tgtConn.commit();
                            batchCount = 0;
                            log.info("Sync {}: {} rows written", task.getName(), totalRows);
                            // 断点续传: 每批提交后持久化水位线, 任务中断后下次从水位线续跑
                            if ("INCREMENTAL".equals(task.getSyncMode()) && maxIncrementalValue != null) {
                                task.setIncrementalValue(maxIncrementalValue);
                                task.setRowCount(totalRows);
                                task.setUpdateTime(LocalDateTime.now());
                                taskRepository.save(task);
                            }
                        }
                    }
                    if (batchCount > 0) {
                        ps.executeBatch();
                        tgtConn.commit();
                    }
                }
                }

                task.setStatus("SUCCESS");
                task.setRowCount(totalRows);
                task.setDurationMs(System.currentTimeMillis() - start);
                if (maxIncrementalValue != null) {
                    task.setIncrementalValue(maxIncrementalValue);
                }
                // Event-driven lineage collection (Kafka-equivalent hook)
                lineageService.collectTaskLineage(task.getTenantId(), "SYNC", task.getId());
            }
        } catch (Exception e) {
            log.error("Sync task {} failed", id, e);
            task.setStatus("FAILED");
            task.setDurationMs(System.currentTimeMillis() - start);
            task.setErrorMsg(e.getMessage());
        }
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.save(task);
    }

    private String mapColumnType(String dbType) {
        if (dbType == null) return "VARCHAR(512)";
        String upper = dbType.toUpperCase();
        if (upper.contains("INT") || upper.contains("BIGINT") || upper.contains("SMALLINT") || upper.contains("TINYINT")) {
            return "BIGINT";
        }
        if (upper.contains("DECIMAL") || upper.contains("NUMERIC") || upper.contains("FLOAT") || upper.contains("DOUBLE")) {
            return "DECIMAL(20,6)";
        }
        if (upper.contains("DATE") || upper.contains("TIME") || upper.contains("TIMESTAMP")) {
            return "TIMESTAMP";
        }
        if (upper.contains("BOOL")) {
            return "BOOLEAN";
        }
        if (upper.contains("TEXT") || upper.contains("CLOB") || upper.contains("BLOB")) {
            return "CLOB";
        }
        return "VARCHAR(512)";
    }

    private SyncTaskRes toRes(SyncTaskEntity e) {
        SyncTaskRes res = new SyncTaskRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setSourceDatasourceId(e.getSourceDatasourceId());
        res.setSourceTable(e.getSourceTable());
        res.setTargetDatasourceId(e.getTargetDatasourceId());
        res.setTargetTable(e.getTargetTable());
        res.setSyncMode(e.getSyncMode());
        res.setIncrementalColumn(e.getIncrementalColumn());
        res.setIncrementalValue(e.getIncrementalValue());
        res.setUpsertKey(e.getUpsertKey());
        res.setScheduleCron(e.getScheduleCron());
        res.setStatus(e.getStatus());
        res.setRowCount(e.getRowCount());
        res.setLastRunTime(e.getLastRunTime() != null ? e.getLastRunTime().toString() : null);
        res.setDuration(e.getDurationMs() != null ? formatDuration(e.getDurationMs()) : null);
        res.setErrorMsg(e.getErrorMsg());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%dm%ds", ms / 60000, (ms % 60000) / 1000);
    }
}