package com.temporedata.guard.modules.common.view.service;

import cn.hutool.core.util.StrUtil;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.common.view.entity.ViewEntity;
import com.temporedata.guard.modules.common.view.repository.ViewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data view management service.
 * Supports CRUD for virtual views backed by SQL queries.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewService {

    private final ViewRepository viewRepository;
    private final DatasourceRepository datasourceRepository;

    public List<ViewEntity> listViews(String tenantId) {
        return viewRepository.findByTenantId(tenantId);
    }

    public ViewEntity getView(String viewId) {
        return viewRepository.findById(viewId)
                .orElseThrow(() -> new BusinessException("视图不存在"));
    }

    @Transactional
    public ViewEntity createView(ViewEntity view) {
        view.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        view.setStatus("DRAFT");
        view.setCreateDateTime(LocalDateTime.now());
        view.setLastModifiedDateTime(LocalDateTime.now());
        return viewRepository.save(view);
    }

    @Transactional
    public ViewEntity updateView(String viewId, ViewEntity updates) {
        ViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new BusinessException("视图不存在"));
        if (updates.getName() != null) view.setName(updates.getName());
        if (updates.getRemark() != null) view.setRemark(updates.getRemark());
        if (updates.getQuerySql() != null) view.setQuerySql(updates.getQuerySql());
        if (updates.getDatasourceId() != null) view.setDatasourceId(updates.getDatasourceId());
        if (updates.getWebConfig() != null) view.setWebConfig(updates.getWebConfig());
        if (updates.getCardList() != null) view.setCardList(updates.getCardList());
        if (updates.getBackgroundFileId() != null) view.setBackgroundFileId(updates.getBackgroundFileId());
        view.setLastModifiedDateTime(LocalDateTime.now());
        return viewRepository.save(view);
    }

    @Transactional
    public void deleteView(String viewId) {
        ViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new BusinessException("视图不存在"));
        viewRepository.delete(view);
    }

    @Transactional
    public ViewEntity publishView(String viewId) {
        ViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new BusinessException("视图不存在"));
        view.setStatus("PUBLISHED");
        view.setLastModifiedDateTime(LocalDateTime.now());
        return viewRepository.save(view);
    }

    /**
     * Execute the view's SQL query and return results.
     */
    public Map<String, Object> executeView(String viewId) {
        ViewEntity view = viewRepository.findById(viewId)
                .orElseThrow(() -> new BusinessException("视图不存在"));
        if (view.getQuerySql() == null) {
            throw new BusinessException("视图未配置SQL查询");
        }
        DatasourceEntity ds = datasourceRepository.findById(view.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在"));

        try (Connection conn = DriverManager.getConnection(buildJdbcUrl(ds), ds.getUsername(), ds.getPassword());
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(view.getQuerySql())) {

            ResultSetMetaData meta = rs.getMetaData();
            List<Map<String, Object>> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Map<String, Object> col = new HashMap<>();
                col.put("name", meta.getColumnName(i));
                col.put("type", meta.getColumnTypeName(i));
                columns.add(col);
            }

            List<List<Object>> rows = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.add(rs.getObject(i));
                }
                rows.add(row);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("total", rows.size());
            return result;
        } catch (Exception e) {
            log.error("Failed to execute view query: {}", viewId, e);
            throw new BusinessException("视图查询执行失败: " + e.getMessage());
        }
    }

    private String buildJdbcUrl(DatasourceEntity ds) {
        String type = ds.getType();
        String host = ds.getHost();
        Integer port = ds.getPort();
        String database = ds.getDatabase();
        String params = ds.getParams();

        if (StrUtil.isBlank(type) || StrUtil.isBlank(host)) {
            return null;
        }
        switch (type.toUpperCase()) {
            case "MYSQL":
                return "jdbc:mysql://" + host + ":" + (port != null ? port : 3306) + "/" +
                        (database != null ? database : "") +
                        (params != null ? "?" + params : "?useSSL=false&serverTimezone=UTC");
            case "POSTGRESQL":
                return "jdbc:postgresql://" + host + ":" + (port != null ? port : 5432) + "/" +
                        (database != null ? database : "") +
                        (params != null ? "?" + params : "");
            case "H2":
            default:
                // Platform H2 datasources are backed by per-datasource in-memory query DBs
                return "jdbc:h2:mem:temporedata_guard_query_" + ds.getId() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        }
    }
}