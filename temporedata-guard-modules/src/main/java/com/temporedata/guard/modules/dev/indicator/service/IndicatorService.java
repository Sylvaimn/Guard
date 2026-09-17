package com.temporedata.guard.modules.dev.indicator.service;
import com.temporedata.guard.modules.security.changeaudit.service.ChangeAuditService;
import com.temporedata.guard.modules.dev.indicator.entity.IndicatorEntity;
import com.temporedata.guard.modules.dev.indicator.repository.IndicatorRepository;
import com.temporedata.guard.modules.dev.indicator.entity.IndicatorRunEntity;
import com.temporedata.guard.modules.dev.indicator.repository.IndicatorRunRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.indicator.IndicatorLineageRes;
import com.temporedata.guard.api.indicator.IndicatorPageRes;
import com.temporedata.guard.api.indicator.IndicatorReq;
import com.temporedata.guard.api.indicator.IndicatorRes;
import com.temporedata.guard.api.indicator.IndicatorRunRes;
import com.temporedata.guard.api.indicator.IndicatorStatsRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;

import com.temporedata.guard.modules.dev.meta.service.SqlLineageParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Data indicator service: CRUD, real query execution, run history,
 * SQL-based lineage and scheduled execution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorService {

    private final IndicatorRepository indicatorRepository;
    private final IndicatorRunRepository indicatorRunRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final ChangeAuditService changeAuditService;
    private final SqlLineageParser sqlLineageParser;
    private final IndicatorSchedulerService indicatorSchedulerService;

    private final ConcurrentHashMap<String, Object> executeLocks = new ConcurrentHashMap<>();

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    @Value("${zy.indicator.query-timeout-seconds:30}")
    private int queryTimeoutSeconds;

    public IndicatorPageRes listAll(String tenantId, int page, int size) {
        Page<IndicatorEntity> paged = indicatorRepository
                .findByTenantIdOrderByCreateTimeDesc(tenantId, PageRequest.of(page, size));
        IndicatorPageRes res = new IndicatorPageRes();
        res.setTotal(paged.getTotalElements());
        res.setRecords(paged.getContent().stream().map(this::toResWithLatest).collect(Collectors.toList()));
        return res;
    }

    public IndicatorStatsRes stats(String tenantId) {
        IndicatorStatsRes res = new IndicatorStatsRes();
        res.setTotalRuns((int) indicatorRunRepository.countByTenantId(tenantId));
        res.setSuccessRuns((int) indicatorRunRepository.countByTenantIdAndStatus(tenantId, "SUCCESS"));
        res.setFailedRuns((int) indicatorRunRepository.countByTenantIdAndStatus(tenantId, "FAILED"));
        return res;
    }

    @Transactional
    public IndicatorRes create(IndicatorReq req, String tenantId) {
        if (req.getCode() != null && !req.getCode().isBlank()
                && indicatorRepository.findByCode(req.getCode().trim()).isPresent()) {
            throw new BusinessException("指标编码已存在: " + req.getCode().trim());
        }
        IndicatorEntity entity = new IndicatorEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setCode(req.getCode() == null ? null : req.getCode().trim());
        applyReq(entity, req);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        entity = indicatorRepository.save(entity);
        indicatorSchedulerService.sync(entity);
        return toRes(entity);
    }

    @Transactional
    public IndicatorRes update(String id, IndicatorReq req, String tenantId) {
        IndicatorEntity entity = requireOwned(id, tenantId);
        if (req.getCode() != null && !req.getCode().isBlank()) {
            String code = req.getCode().trim();
            indicatorRepository.findByCode(code)
                    .filter(e -> !e.getId().equals(id))
                    .ifPresent(e -> {
                        throw new BusinessException("指标编码已存在: " + code);
                    });
            entity.setCode(code);
        } else {
            entity.setCode(null);
        }
        applyReq(entity, req);
        entity.setUpdateTime(LocalDateTime.now());
        entity = indicatorRepository.save(entity);
        indicatorSchedulerService.sync(entity);
        return toRes(entity);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        requireOwned(id, tenantId);
        indicatorSchedulerService.unschedule(id);
        indicatorRunRepository.deleteByIndicatorId(id);
        indicatorRepository.deleteById(id);
    }

    /**
     * Execute the indicator SQL against its datasource and record the run result.
     * Safety: tenant ownership check, read-only SQL guard, query timeout, row limit
     * and per-indicator concurrency lock.
     */
    public IndicatorRunRes execute(String id, String tenantId) {
        IndicatorEntity entity = requireOwned(id, tenantId);
        return toRunRes(executeLocked(entity));
    }

    /**
     * Scheduled execution: no login context, tenant ownership comes from the
     * entity itself. Skipped if the indicator is disabled.
     */
    public IndicatorRunRes executeScheduled(String id) {
        IndicatorEntity entity = indicatorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("指标不存在: " + id));
        if (!"ENABLED".equals(entity.getStatus())) {
            throw new BusinessException("指标已停用，无法调度执行");
        }
        return toRunRes(executeLocked(entity));
    }

    public IndicatorEntity findEntity(String id) {
        return indicatorRepository.findById(id).orElse(null);
    }

    private IndicatorRunEntity executeLocked(IndicatorEntity entity) {
        Object lock = executeLocks.computeIfAbsent(entity.getId(), k -> new Object());
        synchronized (lock) {
            return doExecute(entity);
        }
    }

    private IndicatorRunEntity doExecute(IndicatorEntity entity) {
        IndicatorRunEntity run = new IndicatorRunEntity();
        run.setId(UUID.randomUUID().toString().replace("-", ""));
        run.setIndicatorId(entity.getId());
        run.setUnit(entity.getUnit());
        run.setExecuteTime(LocalDateTime.now());
        run.setTenantId(entity.getTenantId());

        long start = System.currentTimeMillis();
        try {
            String sql = entity.getQuerySql() == null ? "" : entity.getQuerySql().trim();
            String head = sql.toUpperCase();
            if (!head.startsWith("SELECT") && !head.startsWith("WITH")) {
                throw new BusinessException("指标 SQL 仅支持只读查询（SELECT / WITH 开头），当前语句已被拒绝");
            }
            DatasourceEntity ds = datasourceRepository.findById(entity.getDatasourceId())
                    .orElseThrow(() -> new BusinessException("指标数据源不存在: " + entity.getDatasourceId()));
            if (!entity.getTenantId().equals(ds.getTenantId())) {
                throw new BusinessException("指标数据源不属于当前租户，已拒绝执行");
            }
            String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
            DataSource dataSource = connectionManager.getOrCreate(ds, plainPwd);

            String value;
            try (Connection conn = dataSource.getConnection();
                 Statement st = conn.createStatement()) {
                st.setQueryTimeout(queryTimeoutSeconds);
                st.setMaxRows(1);
                try (ResultSet rs = st.executeQuery(sql)) {
                    value = rs.next() && rs.getString(1) != null ? rs.getString(1) : "0";
                }
            }
            run.setResultValue(value);
            run.setStatus("SUCCESS");
        } catch (Exception e) {
            log.warn("Indicator [{}] execution failed: {}", entity.getCode(), e.getMessage());
            run.setStatus("FAILED");
            run.setErrorMsg(e.getMessage() != null && e.getMessage().length() > 1000
                    ? e.getMessage().substring(0, 1000) : e.getMessage());
        }
        run.setDurationMs(System.currentTimeMillis() - start);

        indicatorRunRepository.save(run);
        changeAuditService.record("INDICATOR", entity.getId(), entity.getName(), "EXECUTE",
                null, null, "status=" + run.getStatus() + ",value=" + run.getResultValue(), entity.getTenantId());
        return run;
    }

    public List<IndicatorRunRes> listRuns(String indicatorId, String tenantId) {
        requireOwned(indicatorId, tenantId);
        return indicatorRunRepository.findByIndicatorIdOrderByExecuteTimeDesc(indicatorId).stream()
                .map(this::toRunRes)
                .collect(Collectors.toList());
    }

    /**
     * Lineage of an indicator: tables referenced by its SQL plus upstream
     * indicators (same tenant) that read at least one of those tables.
     */
    public IndicatorLineageRes lineage(String indicatorId, String tenantId) {
        IndicatorEntity entity = requireOwned(indicatorId, tenantId);
        IndicatorLineageRes res = new IndicatorLineageRes();

        Set<String> tables = new HashSet<>();
        com.temporedata.guard.api.meta.SqlLineageRes parsed = sqlLineageParser.parse(entity.getQuerySql());
        if (parsed != null) {
            tables.addAll(parsed.getInputTables());
        }
        res.setTables(tables.stream().sorted().collect(Collectors.toList()));

        List<IndicatorEntity> all = indicatorRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
        for (IndicatorEntity other : all) {
            if (other.getId().equals(entity.getId()) || other.getQuerySql() == null) continue;
            com.temporedata.guard.api.meta.SqlLineageRes otherParsed = sqlLineageParser.parse(other.getQuerySql());
            if (otherParsed == null) continue;
            Set<String> otherTables = new HashSet<>(otherParsed.getInputTables());
            if (otherTables.stream().anyMatch(tables::contains)) {
                IndicatorLineageRes.UpstreamIndicator up = new IndicatorLineageRes.UpstreamIndicator();
                up.setId(other.getId());
                up.setName(other.getName());
                up.setCode(other.getCode());
                up.setType(other.getType());
                up.setStatus(other.getStatus());
                res.getUpstream().add(up);
            }
        }
        return res;
    }

    private IndicatorEntity requireOwned(String id, String tenantId) {
        IndicatorEntity entity = indicatorRepository.findById(id)
                .orElseThrow(() -> new BusinessException("指标不存在"));
        if (!entity.getTenantId().equals(tenantId)) {
            throw new BusinessException("指标不存在或无权访问");
        }
        return entity;
    }

    private void applyReq(IndicatorEntity entity, IndicatorReq req) {
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setDescription(req.getDescription());
        entity.setQuerySql(req.getQuerySql());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setUnit(req.getUnit());
        entity.setVersion(req.getVersion());
        entity.setTheme(req.getTheme());
        entity.setOwner(req.getOwner());
        entity.setStatus(req.getStatus() == null ? "ENABLED" : req.getStatus());
        entity.setScheduleCron(req.getScheduleCron());
        entity.setScheduleEnabled(req.getScheduleEnabled() == null ? Boolean.FALSE : req.getScheduleEnabled());
    }

    private IndicatorRunRes toRunRes(IndicatorRunEntity e) {
        IndicatorRunRes res = new IndicatorRunRes();
        res.setId(e.getId());
        res.setIndicatorId(e.getIndicatorId());
        res.setResultValue(e.getResultValue());
        res.setUnit(e.getUnit());
        res.setDurationMs(e.getDurationMs());
        res.setStatus(e.getStatus());
        res.setErrorMsg(e.getErrorMsg());
        res.setExecuteTime(e.getExecuteTime() != null ? e.getExecuteTime().toString() : null);
        return res;
    }

    private IndicatorRes toResWithLatest(IndicatorEntity e) {
        IndicatorRes res = toRes(e);
        indicatorRunRepository.findTopByIndicatorIdOrderByExecuteTimeDesc(e.getId()).ifPresent(run -> {
            res.setLatestStatus(run.getStatus());
            res.setLatestValue(run.getResultValue());
            res.setLatestExecuteTime(run.getExecuteTime() != null ? run.getExecuteTime().toString() : null);
            res.setLatestDurationMs(run.getDurationMs());
            res.setLatestErrorMsg(run.getErrorMsg());
        });
        return res;
    }

    private IndicatorRes toRes(IndicatorEntity e) {
        IndicatorRes res = new IndicatorRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setCode(e.getCode());
        res.setType(e.getType());
        res.setDescription(e.getDescription());
        res.setQuerySql(e.getQuerySql());
        res.setDatasourceId(e.getDatasourceId());
        res.setUnit(e.getUnit());
        res.setTenantId(e.getTenantId());
        res.setVersion(e.getVersion());
        res.setTheme(e.getTheme());
        res.setOwner(e.getOwner());
        res.setStatus(e.getStatus());
        res.setScheduleCron(e.getScheduleCron());
        res.setScheduleEnabled(e.getScheduleEnabled());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }
}