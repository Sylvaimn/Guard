package com.temporedata.guard.modules.data.service.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.service.DataApiReq;
import com.temporedata.guard.api.service.DataApiRes;
import com.temporedata.guard.api.service.DataApiTestReq;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.data.service.entity.DataApiEntity;
import com.temporedata.guard.modules.data.service.repository.DataApiRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data API service: publish SQL queries as REST endpoints.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataApiService {

    private final DataApiRepository apiRepository;
    private final DatasourceRepository datasourceRepository;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final com.temporedata.guard.modules.common.eventbus.EventBus eventBus;
    private final JdbcConnectionManager connectionManager;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    private static final java.util.regex.Pattern PLACEHOLDER =
            java.util.regex.Pattern.compile("\\$\\{(\\w+)}");

    /** 参数绑定结果：占位符已替换为 ? 的 SQL + 按序绑定的值 */
    private static class BoundSql {
        final String sql;
        final List<Object> values;
        BoundSql(String sql, List<Object> values) {
            this.sql = sql;
            this.values = values;
        }
    }

    /** 将 ${param} 占位符替换为 PreparedStatement 参数（防 SQL 注入） */
    private BoundSql bindPlaceholders(String sql, Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            return new BoundSql(sql, List.of());
        }
        java.util.regex.Matcher m = PLACEHOLDER.matcher(sql);
        StringBuffer sb = new StringBuffer();
        List<Object> values = new ArrayList<>();
        while (m.find()) {
            String key = m.group(1);
            Object value = params.get(key);
            if (value == null) {
                throw new BusinessException("缺少API参数: " + key);
            }
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("?"));
            values.add(value);
        }
        m.appendTail(sb);
        return new BoundSql(sb.toString(), values);
    }

    public List<DataApiRes> list(String tenantId) {
        return apiRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public DataApiRes detail(String id) {
        return toRes(apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API不存在")));
    }

    @Transactional
    public DataApiRes create(DataApiReq req, String tenantId) {
        // Check path uniqueness
        if (apiRepository.findByPath(req.getPath()).isPresent()) {
            throw new BusinessException("API路径已存在: " + req.getPath());
        }

        DataApiEntity entity = new DataApiEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setSql(req.getSql());
        entity.setMethod(req.getMethod() != null ? req.getMethod().toUpperCase() : "GET");
        entity.setPath(req.getPath());
        entity.setServiceTarget(req.getServiceTarget());
        entity.setDataScope(req.getDataScope());
        entity.setMaskColumns(req.getMaskColumns());
        entity.setQpsLimit(req.getQpsLimit());
        entity.setDailyLimit(req.getDailyLimit());
        entity.setApiKey(generateApiKey());
        entity.setStatus("DRAFT");
        entity.setCacheTtl(req.getCacheTtl() != null ? req.getCacheTtl() : "0");
        entity.setCallCount(0L);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(apiRepository.save(entity));
    }

    @Transactional
    public DataApiRes update(String id, DataApiReq req) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API不存在"));

        // Check path uniqueness if changed
        if (!entity.getPath().equals(req.getPath()) && apiRepository.findByPath(req.getPath()).isPresent()) {
            throw new BusinessException("API路径已存在: " + req.getPath());
        }

        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setSql(req.getSql());
        entity.setMethod(req.getMethod() != null ? req.getMethod().toUpperCase() : "GET");
        entity.setPath(req.getPath());
        entity.setCacheTtl(req.getCacheTtl() != null ? req.getCacheTtl() : "0");
        entity.setServiceTarget(req.getServiceTarget());
        entity.setDataScope(req.getDataScope());
        entity.setMaskColumns(req.getMaskColumns());
        entity.setQpsLimit(req.getQpsLimit());
        entity.setDailyLimit(req.getDailyLimit());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(apiRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        apiRepository.deleteById(id);
    }

    @Transactional
    public DataApiRes toggleStatus(String id) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API不存在"));
        entity.setStatus("ACTIVE".equals(entity.getStatus()) ? "DISABLED" : "ACTIVE");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(apiRepository.save(entity));
    }

    @Transactional
    public DataApiRes submitForApproval(String id) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("接口不存在"));
        if (!"DRAFT".equals(entity.getStatus()) && !"REJECTED".equals(entity.getStatus())) {
            throw new BusinessException("仅草稿/被驳回的接口可提交审批");
        }
        entity.setStatus("PENDING");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(apiRepository.save(entity));
    }

    @Transactional
    public DataApiRes approve(String id) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("接口不存在"));
        if (!"PENDING".equals(entity.getStatus())) {
            throw new BusinessException("仅待审批接口可通过");
        }
        entity.setStatus("ACTIVE");
        entity.setUpdateTime(LocalDateTime.now());
        DataApiRes res = toRes(apiRepository.save(entity));
        try {
            eventBus.publish("tdg-lineage", "API_APPROVED", java.util.Map.of(
                    "apiId", entity.getId(), "apiName", entity.getName(),
                    "sql", entity.getSql() == null ? "" : entity.getSql(),
                    "tenantId", entity.getTenantId() == null ? "DEFAULT" : entity.getTenantId()));
        } catch (Exception ex) {
            log.warn("publish api lineage event failed: {}", ex.getMessage());
        }
        return res;
    }

    /** 接口测试/调用后发布血缘事件(跨层血缘: 表→接口出口), 事件驱动 KafkaEventListener 登记 */
    private void publishLineage(DataApiEntity entity, String sql) {
        try {
            eventBus.publish("tdg-lineage", "API_TEST", java.util.Map.of(
                    "apiId", entity.getId(), "apiName", entity.getName(),
                    "sql", sql == null ? "" : sql,
                    "tenantId", entity.getTenantId() == null ? "DEFAULT" : entity.getTenantId()));
        } catch (Exception ex) {
            log.warn("publish api lineage event failed: {}", ex.getMessage());
        }
    }

    @Transactional
    public DataApiRes reject(String id) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("接口不存在"));
        entity.setStatus("REJECTED");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(apiRepository.save(entity));
    }

    @Transactional
    public DataApiRes regenerateKey(String id) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API不存在"));
        entity.setApiKey(generateApiKey());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(apiRepository.save(entity));
    }

    /**
     * Test execute a data API with parameters.
     */
    public Object testExecute(String id, DataApiTestReq req) {
        DataApiEntity entity = apiRepository.findById(id)
                .orElseThrow(() -> new BusinessException("API不存在"));

        DatasourceEntity ds = datasourceRepository.findById(entity.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        javax.sql.DataSource dataSource = connectionManager.getOrCreate(ds, plainPwd);

        String sql = entity.getSql();
        // 参数化替换 ${param} 占位符（防 SQL 注入）
        BoundSql bound = bindPlaceholders(sql, req.getParams());

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(bound.sql)) {
            for (int i = 0; i < bound.values.size(); i++) {
                ps.setObject(i + 1, bound.values.get(i));
            }

            boolean isQuery = bound.sql.trim().toUpperCase().startsWith("SELECT");
            if (isQuery) {
                try (ResultSet rs = ps.executeQuery()) {
                    ResultSetMetaData meta = rs.getMetaData();
                    List<Map<String, Object>> rows = new ArrayList<>();
                    List<String> columns = new ArrayList<>();
                    for (int i = 1; i <= meta.getColumnCount(); i++) {
                        columns.add(meta.getColumnName(i));
                    }
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (String col : columns) {
                            row.put(col, rs.getObject(col));
                        }
                        rows.add(row);
                    }
                    // 脱敏预览: 按接口配置脱敏列打码
                    applyMask(entity.getMaskColumns(), columns, rows);
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("columns", columns);
                    result.put("rows", rows);
                    result.put("total", rows.size());
                    publishLineage(entity, sql);
                    return result;
                }
            } else {
                int affected = ps.executeUpdate();
                return Map.of("affected", affected);
            }
        } catch (Exception e) {
            log.error("Data API test failed: {}", id, e);
            throw new BusinessException("API执行失败: " + e.getMessage());
        }
    }

    /**
     * 动态脱敏引擎: 按调用方数据等级对敏感列打码(外部调用默认低等级 -> 脱敏)
     */
    private void applyMask(String maskColumns, List<String> columns, List<Map<String, Object>> rows) {
        if (maskColumns == null || maskColumns.trim().isEmpty() || rows.isEmpty()) return;
        Set<String> cols = new HashSet<>(Arrays.asList(maskColumns.split(",")));
        for (Map<String, Object> row : rows) {
            for (Map.Entry<String, Object> e : row.entrySet()) {
                if (cols.contains(e.getKey()) && e.getValue() != null) {
                    e.setValue(maskValue(String.valueOf(e.getValue())));
                }
            }
        }
    }

    private String maskValue(String v) {
        if (v.length() <= 7) return v.substring(0, 1) + "***";
        return v.substring(0, 3) + "****" + v.substring(v.length() - 4);
    }

    /** 配额校验: 每日调用上限拦截 */
    private void enforceDailyQuota(DataApiEntity entity) {
        Integer limit = entity.getDailyLimit();
        if (limit == null || limit <= 0) return;
        String today = LocalDate.now().toString();
        if (!today.equals(entity.getTodayDate())) {
            entity.setTodayDate(today);
            entity.setTodayCalls(0L);
        }
        long used = entity.getTodayCalls() != null ? entity.getTodayCalls() : 0;
        if (used >= limit) {
            throw new BusinessException("接口今日调用配额已用尽(" + limit + ")，请联系管理员扩容");
        }
        if (used >= limit * 0.9) {
            log.warn("Data API {} quota nearly exhausted: {}/{}", entity.getPath(), used, limit);
        }
    }

    /**
     * Public API execution by path + apiKey.
     */
    public Object executeByKey(String path, String apiKey, Map<String, Object> params) {
        io.micrometer.core.instrument.Counter.builder("data_api_call_total").description("警务数据接口调用量")
                .tag("path", path).register(meterRegistry).increment();
        DataApiEntity entity = apiRepository.findByPathAndApiKey(path, apiKey)
                .orElseThrow(() -> new BusinessException("API不存在或密钥无效"));

        if (!"ACTIVE".equals(entity.getStatus())) {
            throw new BusinessException("API未发布或已停用");
        }
        enforceDailyQuota(entity);

        DatasourceEntity ds = datasourceRepository.findById(entity.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        javax.sql.DataSource dataSource = connectionManager.getOrCreate(ds, plainPwd);

        String sql = entity.getSql();
        BoundSql bound = bindPlaceholders(sql, params);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(bound.sql)) {
            for (int i = 0; i < bound.values.size(); i++) {
                ps.setObject(i + 1, bound.values.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            List<Map<String, Object>> rows = new ArrayList<>();
            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                columns.add(meta.getColumnName(i));
            }
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (String col : columns) {
                    row.put(col, rs.getObject(col));
                }
                rows.add(row);
            }

            // Update call count
            entity.setCallCount(entity.getCallCount() != null ? entity.getCallCount() + 1 : 1);
            entity.setLastCallTime(LocalDateTime.now());
            String today = LocalDate.now().toString();
            if (!today.equals(entity.getTodayDate())) { entity.setTodayDate(today); entity.setTodayCalls(0L); }
            entity.setTodayCalls(entity.getTodayCalls() != null ? entity.getTodayCalls() + 1 : 1);
            apiRepository.save(entity);

            // 动态脱敏: 外部调用按低等级脱敏
            applyMask(entity.getMaskColumns(), columns, rows);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("columns", columns);
            result.put("rows", rows);
            result.put("total", rows.size());
            return result;
            }
        } catch (Exception e) {
            log.error("Data API execution failed: {}", path, e);
            throw new BusinessException("API执行失败: " + e.getMessage());
        }
    }

    private String generateApiKey() {
        return "dak-" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
    }

    private DataApiRes toRes(DataApiEntity e) {
        DataApiRes res = new DataApiRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setDescription(e.getDescription());
        res.setDatasourceId(e.getDatasourceId());
        res.setSql(e.getSql());
        res.setMethod(e.getMethod());
        res.setPath(e.getPath());
        // Mask API key, only show first 8 chars
        res.setApiKey(e.getApiKey() != null && e.getApiKey().length() > 8
                ? e.getApiKey().substring(0, 8) + "****" : e.getApiKey());
        res.setStatus(e.getStatus());
        res.setCacheTtl(e.getCacheTtl());
        res.setCallCount(e.getCallCount());
        res.setLastCallTime(e.getLastCallTime() != null ? e.getLastCallTime().toString() : null);
        res.setServiceTarget(e.getServiceTarget());
        res.setDataScope(e.getDataScope());
        res.setMaskColumns(e.getMaskColumns());
        res.setQpsLimit(e.getQpsLimit());
        res.setDailyLimit(e.getDailyLimit());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }
}