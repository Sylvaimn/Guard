package com.temporedata.guard.modules.dev.ingestion.service;
import com.temporedata.guard.modules.dev.lineage.service.LineageService;
import com.temporedata.guard.modules.common.async.TaskExecutorService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.ingestion.IngestionTaskReq;
import com.temporedata.guard.api.ingestion.IngestionTaskRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.ingestion.entity.IngestionTaskEntity;
import com.temporedata.guard.modules.dev.ingestion.repository.IngestionTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data ingestion service: CSV upload, auto DDL, data writing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final IngestionTaskRepository taskRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService;
    private final com.temporedata.guard.modules.common.async.TaskExecutorService taskExecutorService;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 异步执行采集任务：请求线程只读文件字节并提交，CSV 解析与写入在线程池执行。
     */
    public boolean executeAsync(String taskId, byte[] fileContent) {
        boolean accepted = taskExecutorService.submit("ingestion:" + taskId, () -> execute(taskId, fileContent));
        if (!accepted) {
            throw new BusinessException("采集任务正在执行中");
        }
        return true;
    }

    public List<IngestionTaskRes> list(String tenantId) {
        return taskRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public IngestionTaskRes detail(String id) {
        return toRes(taskRepository.findById(id)
                .orElseThrow(() -> new BusinessException("采集任务不存在")));
    }

    @Transactional
    public IngestionTaskRes create(IngestionTaskReq req, String tenantId) {
        IngestionTaskEntity entity = new IngestionTaskEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTargetTable(req.getTargetTable());
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

    /**
     * Upload CSV and execute ingestion.
     * 由 TaskExecutorService 异步执行，不占用 HTTP 线程与长事务。
     */
    public void execute(String taskId, byte[] fileContent) {
        IngestionTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("采集任务不存在"));

        task.setStatus("RUNNING");
        task.setLastRunTime(LocalDateTime.now());
        taskRepository.save(task);

        try {
            DatasourceEntity ds = datasourceRepository.findById(task.getDatasourceId())
                    .orElseThrow(() -> new BusinessException("数据源不存在"));
            String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
            javax.sql.DataSource dataSource = connectionManager.getOrCreate(ds, plainPwd);

            // Parse CSV config
            Map<String, String> cfg = parseConfig(task.getConfig());
            String delimiter = cfg.getOrDefault("delimiter", ",");
            boolean hasHeader = Boolean.parseBoolean(cfg.getOrDefault("hasHeader", "true"));
            String encoding = cfg.getOrDefault("encoding", "UTF-8");

            // Read CSV content
            List<String[]> rows = new ArrayList<>();
            String[] headers;
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new java.io.ByteArrayInputStream(fileContent), encoding))) {
                String line = reader.readLine();
                if (hasHeader && line != null) {
                    headers = parseCsvLine(line, delimiter);
                } else {
                    // No header: generate column names col_0, col_1, ...
                    if (line != null) {
                        rows.add(parseCsvLine(line, delimiter));
                    }
                    int colCount = rows.isEmpty() ? 0 : rows.get(0).length;
                    headers = new String[colCount];
                    for (int i = 0; i < colCount; i++) {
                        headers[i] = "col_" + i;
                    }
                }
                while ((line = reader.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    rows.add(parseCsvLine(line, delimiter));
                }
            }

            if (headers == null || headers.length == 0) {
                throw new BusinessException("CSV文件为空");
            }

            try (Connection conn = dataSource.getConnection()) {
                // Drop existing table and create new
                String dropSql = "DROP TABLE IF EXISTS " + task.getTargetTable();
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(dropSql);
                }

                // Auto-detect column types: all VARCHAR(255) for simplicity
                StringBuilder ddl = new StringBuilder("CREATE TABLE " + task.getTargetTable() + " (");
                for (int i = 0; i < headers.length; i++) {
                    if (i > 0) ddl.append(", ");
                    ddl.append(sanitizeColumn(headers[i])).append(" VARCHAR(512)");
                }
                ddl.append(")");
                try (Statement stmt = conn.createStatement()) {
                    stmt.execute(ddl.toString());
                }

                // Insert data
                StringBuilder insertSql = new StringBuilder("INSERT INTO " + task.getTargetTable() + " (");
                for (int i = 0; i < headers.length; i++) {
                    if (i > 0) insertSql.append(", ");
                    insertSql.append(sanitizeColumn(headers[i]));
                }
                insertSql.append(") VALUES (");
                for (int i = 0; i < headers.length; i++) {
                    if (i > 0) insertSql.append(", ");
                    insertSql.append("?");
                }
                insertSql.append(")");

                try (PreparedStatement ps = conn.prepareStatement(insertSql.toString())) {
                    for (String[] row : rows) {
                        for (int i = 0; i < headers.length; i++) {
                            ps.setString(i + 1, i < row.length ? row[i] : "");
                        }
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            }

            task.setStatus("SUCCESS");
            task.setRowCount(rows.size());
            task.setErrorMsg(null);
            lineageService.collectTaskLineage(task.getTenantId(), "INGESTION", task.getId());
        } catch (Exception e) {
            log.error("Ingestion task {} failed", taskId, e);
            task.setStatus("FAILED");
            task.setErrorMsg(e.getMessage());
        }
        task.setUpdateTime(LocalDateTime.now());
        taskRepository.save(task);
    }

    private String[] parseCsvLine(String line, String delimiter) {
        // Simple CSV parsing: split by delimiter, trim whitespace, remove quotes
        return Arrays.stream(line.split(delimiter))
                .map(String::trim)
                .map(s -> s.replaceAll("^\"|\"$", ""))
                .toArray(String[]::new);
    }

    private String sanitizeColumn(String name) {
        // Replace special chars, wrap in quotes if needed
        String sanitized = name.replaceAll("[^a-zA-Z0-9_]", "_");
        if (sanitized.isEmpty()) sanitized = "col";
        if (Character.isDigit(sanitized.charAt(0))) sanitized = "_" + sanitized;
        return "\"" + sanitized + "\"";
    }

    private Map<String, String> parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private IngestionTaskRes toRes(IngestionTaskEntity e) {
        IngestionTaskRes res = new IngestionTaskRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setDatasourceId(e.getDatasourceId());
        res.setTargetTable(e.getTargetTable());
        res.setStatus(e.getStatus());
        res.setRowCount(e.getRowCount());
        res.setLastRunTime(e.getLastRunTime() != null ? e.getLastRunTime().toString() : null);
        res.setErrorMsg(e.getErrorMsg());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }
}