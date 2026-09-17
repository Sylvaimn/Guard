package com.temporedata.guard.modules.dev.quality.service;
import com.temporedata.guard.modules.dev.lineage.service.LineageService;
import com.temporedata.guard.modules.dev.quality.entity.QualityCheckEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityCheckRepository;
import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.audit.AuditReportRes;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.quality.QualityCheckRes;
import com.temporedata.guard.api.quality.QualityRuleReq;
import com.temporedata.guard.api.quality.QualityRuleRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data quality service: rule CRUD and check execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QualityService {

    private final QualityRuleRepository ruleRepository;
    private final QualityCheckRepository checkRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<QualityRuleRes> list(String tenantId) {
        return ruleRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public QualityRuleRes detail(String id) {
        return toRes(ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("质量规则不存在")));
    }

    @Transactional
    public QualityRuleRes create(QualityRuleReq req, String tenantId) {
        QualityRuleEntity entity = new QualityRuleEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTableName(req.getTableName());
        entity.setColumnName(req.getColumnName());
        entity.setRuleType(req.getRuleType());
        entity.setRuleConfig(req.getRuleConfig());
        entity.setDescription(req.getDescription());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(ruleRepository.save(entity));
    }

    @Transactional
    public QualityRuleRes update(String id, QualityRuleReq req) {
        QualityRuleEntity entity = ruleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("质量规则不存在"));
        entity.setName(req.getName());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTableName(req.getTableName());
        entity.setColumnName(req.getColumnName());
        entity.setRuleType(req.getRuleType());
        entity.setRuleConfig(req.getRuleConfig());
        entity.setDescription(req.getDescription());
        return toRes(ruleRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        ruleRepository.deleteById(id);
    }

    /**
     * Execute a quality check on a single rule.
     */
    @org.springframework.transaction.annotation.Transactional
    public void updateSchedule(String ruleId, String cron) {
        QualityRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException("质量规则不存在"));
        rule.setScheduleCron(cron);
        ruleRepository.save(rule);
    }

    public QualityCheckRes execute(String ruleId) {
        long start = System.currentTimeMillis();
        QualityRuleEntity rule = ruleRepository.findById(ruleId)
                .orElseThrow(() -> new BusinessException("质量规则不存在"));

        QualityCheckEntity check = new QualityCheckEntity();
        check.setId(UUID.randomUUID().toString().replace("-", ""));
        check.setRuleId(ruleId);
        check.setCheckTime(LocalDateTime.now());

        try {
            DatasourceEntity ds = datasourceRepository.findById(rule.getDatasourceId())
                    .orElseThrow(() -> new BusinessException("数据源不存在"));
            String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
            javax.sql.DataSource dataSource = connectionManager.getOrCreate(ds, plainPwd);

            String result;
            boolean pass;
            switch (rule.getRuleType()) {
                case "NOT_NULL":
                    result = checkNotNull(dataSource, rule);
                    pass = result.startsWith("PASS");
                    break;
                case "UNIQUE":
                    result = checkUnique(dataSource, rule);
                    pass = result.startsWith("PASS");
                    break;
                case "RANGE":
                    result = checkRange(dataSource, rule);
                    pass = result.startsWith("PASS");
                    break;
                case "REGEX":
                    result = checkRegex(dataSource, rule);
                    pass = result.startsWith("PASS");
                    break;
                case "CUSTOM_SQL":
                    result = checkCustomSql(dataSource, rule);
                    pass = result.startsWith("PASS");
                    break;
                default:
                    throw new BusinessException("未知规则类型: " + rule.getRuleType());
            }

            check.setStatus(pass ? "PASS" : "FAIL");
            check.setResult(result);
            rule.setLastStatus(check.getStatus());
            if (pass) {
                lineageService.collectTaskLineage(rule.getTenantId(), "QUALITY", rule.getId());
            }
        } catch (Exception e) {
            log.error("Quality check failed for rule {}", ruleId, e);
            check.setStatus("FAIL");
            check.setErrorMsg(e.getMessage());
            check.setResult("检查执行异常: " + e.getMessage());
            rule.setLastStatus("FAIL");
        }

        check.setDurationMs(System.currentTimeMillis() - start);
        checkRepository.save(check);
        ruleRepository.save(rule);

        return toCheckRes(check, rule);
    }

    private String checkNotNull(javax.sql.DataSource ds, QualityRuleEntity rule) throws Exception {
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s IS NULL",
                rule.getTableName(), rule.getColumnName());
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long nullCount = rs.getLong(1);
                if (nullCount == 0) {
                    return String.format("PASS: 列 %s 无空值", rule.getColumnName());
                } else {
                    return String.format("FAIL: 列 %s 存在 %d 个空值", rule.getColumnName(), nullCount);
                }
            }
        }
        return "FAIL: 无法执行检查";
    }

    private String checkUnique(javax.sql.DataSource ds, QualityRuleEntity rule) throws Exception {
        String sql = String.format("SELECT %s, COUNT(*) c FROM %s WHERE %s IS NOT NULL GROUP BY %s HAVING COUNT(*) > 1",
                rule.getColumnName(), rule.getTableName(), rule.getColumnName(), rule.getColumnName());
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long dupCount = rs.getLong("c");
                return String.format("FAIL: 列 %s 存在重复值，重复计数 %d", rule.getColumnName(), dupCount);
            } else {
                return String.format("PASS: 列 %s 值唯一", rule.getColumnName());
            }
        }
    }

    private String checkRange(javax.sql.DataSource ds, QualityRuleEntity rule) throws Exception {
        Map<String, Object> cfg = parseConfig(rule.getRuleConfig());
        Object min = cfg.get("min");
        Object max = cfg.get("max");

        StringBuilder sql = new StringBuilder(
                String.format("SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL", rule.getTableName(), rule.getColumnName()));
        if (min != null) sql.append(" AND ").append(rule.getColumnName()).append(" < ").append(min);
        if (max != null) sql.append(" AND ").append(rule.getColumnName()).append(" > ").append(max);

        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long outOfRange = rs.getLong(1);
                if (outOfRange == 0) {
                    return String.format("PASS: 列 %s 值在范围内", rule.getColumnName());
                } else {
                    return String.format("FAIL: 列 %s 存在 %d 条超出范围 [%s, %s]",
                            rule.getColumnName(), outOfRange, min, max);
                }
            }
        }
        return "FAIL: 无法执行检查";
    }

    private String checkRegex(javax.sql.DataSource ds, QualityRuleEntity rule) throws Exception {
        Map<String, Object> cfg = parseConfig(rule.getRuleConfig());
        String pattern = (String) cfg.get("pattern");
        if (pattern == null) {
            return "FAIL: 未配置正则表达式";
        }

        // H2 uses REGEXP_LIKE, MySQL uses REGEXP, PostgreSQL uses ~
        String sql = String.format("SELECT COUNT(*) FROM %s WHERE %s IS NOT NULL AND NOT REGEXP_LIKE(%s, ?)",
                rule.getTableName(), rule.getColumnName(), rule.getColumnName());
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long mismatch = rs.getLong(1);
                if (mismatch == 0) {
                    return String.format("PASS: 列 %s 所有值匹配正则 %s", rule.getColumnName(), pattern);
                } else {
                    return String.format("FAIL: 列 %s 存在 %d 条不匹配正则 %s", rule.getColumnName(), mismatch, pattern);
                }
            }
        }
        return "FAIL: 无法执行检查";
    }

    private String checkCustomSql(javax.sql.DataSource ds, QualityRuleEntity rule) throws Exception {
        String customSql = rule.getRuleConfig();
        if (customSql == null || customSql.isEmpty()) {
            return "FAIL: 未配置自定义SQL";
        }
        try (Connection conn = ds.getConnection();
             PreparedStatement ps = conn.prepareStatement(customSql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long count = rs.getLong(1);
                if (count == 0) {
                    return "PASS: 自定义SQL检查通过，结果计数为0";
                } else {
                    return String.format("FAIL: 自定义SQL检查失败，结果计数为 %d", count);
                }
            }
        }
        return "FAIL: 无法执行检查";
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isEmpty()) return Collections.emptyMap();
        try {
            return objectMapper.readValue(configJson, Map.class);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private QualityRuleRes toRes(QualityRuleEntity e) {
        QualityRuleRes res = new QualityRuleRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setDatasourceId(e.getDatasourceId());
        res.setTableName(e.getTableName());
        res.setColumnName(e.getColumnName());
        res.setRuleType(e.getRuleType());
        res.setRuleConfig(e.getRuleConfig());
        res.setDescription(e.getDescription());
        res.setLastStatus(e.getLastStatus());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private QualityCheckRes toCheckRes(QualityCheckEntity c, QualityRuleEntity r) {
        QualityCheckRes res = new QualityCheckRes();
        res.setCheckId(c.getId());
        res.setRuleName(r.getName());
        res.setStatus(c.getStatus());
        res.setResult(c.getResult());
        res.setDurationMs(c.getDurationMs());
        res.setErrorMsg(c.getErrorMsg());
        res.setCheckTime(c.getCheckTime() != null ? c.getCheckTime().toString() : null);
        return res;
    }

    /** 实时质量检查记录(REALTIME 流式校验, 最近 50 条) */
    public java.util.List<java.util.Map<String, Object>> realtimeChecks() {
        return jdbcTemplate.queryForList(
                "SELECT rule_id AS ruleId, status, result, duration_ms AS durationMs, error_msg AS errorMsg, " +
                        "check_time AS checkTime FROM temporedata_guard_quality_check WHERE rule_id LIKE 'REALTIME:%' " +
                        "ORDER BY check_time DESC LIMIT 50");
    }

    // ===== P3: Batch audit & issue tracking =====

    /**
     * Batch execute all quality rules for a given datasource/table.
     */
    public List<QualityCheckRes> batchExecute(String datasourceId, String tableName) {
        List<QualityRuleEntity> rules;
        if (datasourceId != null && tableName != null) {
            rules = ruleRepository.findByDatasourceIdAndTableName(datasourceId, tableName);
        } else if (datasourceId != null) {
            rules = ruleRepository.findByDatasourceId(datasourceId);
        } else {
            rules = ruleRepository.findAll();
        }

        List<QualityCheckRes> results = new ArrayList<>();
        for (QualityRuleEntity rule : rules) {
            try {
                results.add(execute(rule.getId()));
            } catch (Exception e) {
                log.error("Batch check failed for rule {}", rule.getId(), e);
            }
        }
        return results;
    }

    /**
     * Generate audit report for a datasource/table.
     */
    public AuditReportRes auditReport(String datasourceId, String tableName) {
        List<QualityRuleEntity> rules;
        if (datasourceId != null && tableName != null) {
            rules = ruleRepository.findByDatasourceIdAndTableName(datasourceId, tableName);
        } else if (datasourceId != null) {
            rules = ruleRepository.findByDatasourceId(datasourceId);
        } else {
            rules = ruleRepository.findAll();
        }

        AuditReportRes report = new AuditReportRes();
        report.setDatasourceId(datasourceId);
        report.setTableName(tableName);
        report.setTotalRules(rules.size());
        report.setReportTime(LocalDateTime.now().toString());

        List<AuditReportRes.AuditRuleResult> ruleResults = new ArrayList<>();
        int passCount = 0;
        int failCount = 0;

        for (QualityRuleEntity rule : rules) {
            AuditReportRes.AuditRuleResult rr = new AuditReportRes.AuditRuleResult();
            rr.setRuleName(rule.getName());
            rr.setRuleType(rule.getRuleType());

            // Get latest check result
            List<QualityCheckEntity> checks = checkRepository.findByRuleIdOrderByCheckTimeDesc(rule.getId());
            if (!checks.isEmpty()) {
                QualityCheckEntity latest = checks.get(0);
                rr.setStatus(latest.getStatus());
                rr.setResult(latest.getResult());
                rr.setCheckTime(latest.getCheckTime() != null ? latest.getCheckTime().toString() : null);
                if ("PASS".equals(latest.getStatus())) {
                    passCount++;
                } else {
                    failCount++;
                }
            } else {
                rr.setStatus("UNCHECKED");
                rr.setResult("未执行检查");
            }
            ruleResults.add(rr);
        }

        report.setPassCount(passCount);
        report.setFailCount(failCount);
        if (rules.size() > 0) {
            report.setPassRate(String.format("%.1f%%", passCount * 100.0 / rules.size()));
        } else {
            report.setPassRate("N/A");
        }
        report.setRuleResults(ruleResults);

        return report;
    }

}
