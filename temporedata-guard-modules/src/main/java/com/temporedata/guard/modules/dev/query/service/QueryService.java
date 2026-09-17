package com.temporedata.guard.modules.dev.query.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.query.QueryLogRes;
import com.temporedata.guard.api.query.QueryReq;
import com.temporedata.guard.api.query.QueryRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.query.entity.QueryLogEntity;
import com.temporedata.guard.modules.dev.query.repository.QueryLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QueryService {

    private final QueryLogRepository queryLogRepository;

    private final DatasourceRepository datasourceRepository;

    private final JdbcConnectionManager connectionManager;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    private static final int MAX_ROWS = 1000;
    private static final int QUERY_TIMEOUT_SECONDS = 30;

    public QueryRes execute(QueryReq req, String userId, String tenantId) {
        DatasourceEntity ds = datasourceRepository.findById(req.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        // 租户归属校验：只能查询本租户数据源
        if (ds.getTenantId() != null && !ds.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权访问该数据源");
        }

        // 安全控制：只允许 SELECT 查询，禁止 DML/DDL 直通
        String sql = req.getSql() == null ? "" : req.getSql().trim();
        if (sql.isEmpty()) {
            throw new BusinessException("SQL不能为空");
        }
        if (!isSelectOnly(sql)) {
            throw new BusinessException("仅允许执行 SELECT 查询");
        }

        String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        DataSource pool = connectionManager.getOrCreate(ds, plainPwd);

        long start = System.currentTimeMillis();
        QueryLogEntity log = new QueryLogEntity();
        log.setId(UUID.randomUUID().toString().replace("-", ""));
        log.setDatasourceId(ds.getId());
        log.setDatasourceName(ds.getName());
        log.setSql(req.getSql());
        log.setUserId(userId);
        log.setTenantId(tenantId);
        log.setCreateTime(LocalDateTime.now());

        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            stmt.setMaxRows(MAX_ROWS);

            boolean isResultSet = stmt.execute(req.getSql());
            QueryRes result = new QueryRes();
            result.setColumns(new ArrayList<>());
            result.setRows(new ArrayList<>());

            if (isResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    for (int i = 1; i <= colCount; i++) {
                        result.getColumns().add(meta.getColumnLabel(i));
                    }
                    while (rs.next()) {
                        List<Object> row = new ArrayList<>();
                        for (int i = 1; i <= colCount; i++) {
                            row.add(rs.getObject(i));
                        }
                        result.getRows().add(row);
                    }
                }
                result.setRowCount(result.getRows().size());
            } else {
                int updateCount = stmt.getUpdateCount();
                result.getColumns().add("affected_rows");
                result.getRows().add(List.of(updateCount));
                result.setRowCount(1);
            }

            long duration = System.currentTimeMillis() - start;
            result.setDurationMs(duration);

            log.setStatus("SUCCESS");
            log.setDurationMs(duration);
            queryLogRepository.save(log);

            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.setStatus("FAILED");
            log.setDurationMs(duration);
            log.setErrorMsg(e.getMessage());
            queryLogRepository.save(log);
            throw new BusinessException("SQL执行失败: " + e.getMessage());
        }
    }

    /** 校验 SQL 为纯 SELECT（去注释后仅允许 SELECT/CTE/WITH 开头） */
    private boolean isSelectOnly(String sql) {
        String cleaned = sql.replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("--[^\n]*", " ")
                .trim().toLowerCase();
        return cleaned.startsWith("select")
                || cleaned.startsWith("with")
                || cleaned.startsWith("(");
    }

    public List<QueryLogRes> history(String tenantId) {
        return queryLogRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    private QueryLogRes toRes(QueryLogEntity e) {
        QueryLogRes res = new QueryLogRes();
        res.setId(e.getId());
        res.setDatasourceName(e.getDatasourceName());
        res.setSql(e.getSql());
        res.setStatus(e.getStatus());
        res.setDurationMs(e.getDurationMs());
        res.setCreateTime(e.getCreateTime().toString());
        res.setErrorMsg(e.getErrorMsg());
        return res;
    }
}