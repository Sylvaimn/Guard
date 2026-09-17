package com.temporedata.guard.modules.dev.dashboard.service;

import com.temporedata.guard.api.dashboard.DashboardRes;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.catalog.entity.TableMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository;
import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;
import com.temporedata.guard.modules.data.service.repository.DataApiRepository;
import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;

import com.temporedata.guard.modules.dev.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dashboard service: aggregate stats from all modules.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DatasourceRepository datasourceRepository;
    private final TableMetadataRepository tableMetadataRepository;
    private final SyncTaskRepository syncTaskRepository;
    private final QualityRuleRepository qualityRuleRepository;
    private final WorkflowRepository workflowRepository;
    private final DataApiRepository dataApiRepository;

    public DashboardRes overview() {
        DashboardRes res = new DashboardRes();

        // Datasource count
        res.setDatasourceCount((int) datasourceRepository.count());

        // Table metadata (collected via structure sync)
        List<TableMetadataEntity> tables = tableMetadataRepository.findAll();
        res.setTableCount(tables.size());
        res.setTotalRows(tables.stream().mapToLong(t -> t.getRowCount() != null ? t.getRowCount() : 0).sum());

        // Sync tasks (计数走 SQL, 失败明细列表保留分页前的最小扫描)
        List<SyncTaskEntity> syncTasks = syncTaskRepository.findAll();
        res.setSyncTaskCount(syncTasks.size());
        res.setSyncSuccessCount((int) syncTaskRepository.countByStatus("SUCCESS"));
        res.setSyncFailCount((int) syncTaskRepository.countByStatus("FAILED"));

        List<DashboardRes.StatItem> syncStatus = new ArrayList<>();
        syncStatus.add(stat("READY", (int) syncTaskRepository.countByStatus("READY")));
        syncStatus.add(stat("RUNNING", (int) syncTaskRepository.countByStatus("RUNNING")));
        syncStatus.add(stat("SUCCESS", res.getSyncSuccessCount()));
        syncStatus.add(stat("FAILED", res.getSyncFailCount()));
        res.setSyncStatus(syncStatus);

        // Quality rules
        List<QualityRuleEntity> rules = qualityRuleRepository.findAll();
        res.setQualityRuleCount(rules.size());
        res.setQualityPassCount((int) qualityRuleRepository.countByLastStatus("PASS"));
        res.setQualityFailCount((int) qualityRuleRepository.countByLastStatus("FAIL"));

        List<DashboardRes.StatItem> qualityStatus = new ArrayList<>();
        qualityStatus.add(stat("PASS", res.getQualityPassCount()));
        qualityStatus.add(stat("FAIL", res.getQualityFailCount()));
        res.setQualityStatus(qualityStatus);

        // Workflows
        res.setWorkflowCount((int) workflowRepository.count());

        // Data APIs
        res.setApiCount((int) dataApiRepository.count());
        res.setApiCallTotal(dataApiRepository.findAll().stream()
                .mapToLong(a -> a.getCallCount() != null ? a.getCallCount() : 0)
                .sum());

        // Top tables by row count
        res.setTopTables(tables.stream()
                .sorted((a, b) -> Long.compare(
                        b.getRowCount() != null ? b.getRowCount() : 0,
                        a.getRowCount() != null ? a.getRowCount() : 0))
                .limit(10)
                .map(t -> {
                    DashboardRes.TableStat ts = new DashboardRes.TableStat();
                    ts.setTableName(t.getTableName());
                    ts.setRowCount(t.getRowCount() != null ? t.getRowCount() : 0);
                    return ts;
                })
                .collect(Collectors.toList()));

        // Recent sync tasks
        res.setRecentSyncs(syncTasks.stream()
                .sorted((a, b) -> {
                    if (a.getLastRunTime() == null) return 1;
                    if (b.getLastRunTime() == null) return -1;
                    return b.getLastRunTime().compareTo(a.getLastRunTime());
                })
                .limit(10)
                .map(t -> {
                    DashboardRes.SyncStat ss = new DashboardRes.SyncStat();
                    ss.setTaskName(t.getName());
                    ss.setStatus(t.getStatus());
                    ss.setLastRunTime(t.getLastRunTime() != null ? t.getLastRunTime().toString() : null);
                    ss.setRowCount(t.getRowCount());
                    return ss;
                })
                .collect(Collectors.toList()));

        // Hourly trend (last 24h)
        fillHourlyTrend(res, syncTasks);

        // Datasource type distribution
        fillDatasourceTypeDist(res);

        // Failed sync tasks
        fillFailedSyncs(res, syncTasks);

        // Failed quality rules
        fillFailedRules(res, rules);

        // Datasource list
        fillDatasources(res);

        return res;
    }

    private DashboardRes.StatItem stat(String name, int value) {
        DashboardRes.StatItem item = new DashboardRes.StatItem();
        item.setName(name);
        item.setValue(value);
        return item;
    }

    private void fillHourlyTrend(DashboardRes res, List<SyncTaskEntity> syncTasks) {
        List<DashboardRes.TrendItem> trend = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (int i = 23; i >= 0; i--) {
            LocalDateTime hourStart = now.minusHours(i);
            DashboardRes.TrendItem item = new DashboardRes.TrendItem();
            item.setHour(hourStart.format(DateTimeFormatter.ofPattern("HH:00")));
            for (SyncTaskEntity t : syncTasks) {
                if (t.getLastRunTime() == null) continue;
                if (t.getLastRunTime().getHour() == hourStart.getHour()
                        && t.getLastRunTime().toLocalDate().equals(hourStart.toLocalDate())) {
                    if ("SUCCESS".equals(t.getStatus())) item.setSuccess(item.getSuccess() + 1);
                    else if ("FAILED".equals(t.getStatus())) item.setFailed(item.getFailed() + 1);
                }
            }
            trend.add(item);
        }
        res.setHourlyTrend(trend);
    }

    private void fillDatasourceTypeDist(DashboardRes res) {
        Map<String, Long> dist = datasourceRepository.findAll().stream()
                .collect(Collectors.groupingBy(d -> d.getType() != null ? d.getType() : "OTHER", Collectors.counting()));
        List<DashboardRes.StatItem> items = dist.entrySet().stream()
                .map(e -> stat(e.getKey(), e.getValue().intValue()))
                .collect(Collectors.toList());
        res.setDatasourceTypeDist(items);
    }

    private void fillFailedSyncs(DashboardRes res, List<SyncTaskEntity> syncTasks) {
        List<DashboardRes.FailedTask> failed = syncTasks.stream()
                .filter(t -> "FAILED".equals(t.getStatus()) && t.getLastRunTime() != null)
                .sorted((a, b) -> b.getLastRunTime().compareTo(a.getLastRunTime()))
                .limit(10)
                .map(t -> {
                    DashboardRes.FailedTask ft = new DashboardRes.FailedTask();
                    ft.setTaskName(t.getName());
                    ft.setTargetTable(t.getTargetTable());
                    ft.setLastRunTime(t.getLastRunTime().toString());
                    ft.setErrorMsg(t.getErrorMsg());
                    return ft;
                })
                .collect(Collectors.toList());
        res.setFailedSyncs(failed);
    }

    private void fillFailedRules(DashboardRes res, List<QualityRuleEntity> rules) {
        List<DashboardRes.FailRule> failed = rules.stream()
                .filter(r -> "FAIL".equals(r.getLastStatus()))
                .sorted((a, b) -> {
                    if (b.getCreateTime() == null) return 1;
                    if (a.getCreateTime() == null) return -1;
                    return b.getCreateTime().compareTo(a.getCreateTime());
                })
                .limit(10)
                .map(r -> {
                    DashboardRes.FailRule fr = new DashboardRes.FailRule();
                    fr.setRuleName(r.getName());
                    fr.setTableName(r.getTableName());
                    fr.setColumnName(r.getColumnName());
                    fr.setRuleType(r.getRuleType());
                    fr.setDescription(r.getDescription());
                    return fr;
                })
                .collect(Collectors.toList());
        res.setFailedRules(failed);
    }


    private void fillDatasources(DashboardRes res) {
        List<DashboardRes.DataSourceStat> list = datasourceRepository.findAll().stream()
                .map(d -> {
                    DashboardRes.DataSourceStat ds = new DashboardRes.DataSourceStat();
                    ds.setName(d.getName());
                    ds.setType(d.getType());
                    return ds;
                })
                .collect(Collectors.toList());
        res.setDatasources(list);
    }
}