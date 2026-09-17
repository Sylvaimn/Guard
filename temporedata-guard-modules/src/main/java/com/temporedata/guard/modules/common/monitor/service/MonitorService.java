package com.temporedata.guard.modules.common.monitor.service;
import com.temporedata.guard.modules.common.cluster.repository.ClusterNodeRepository;
import com.temporedata.guard.modules.common.cluster.repository.ClusterRepository;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowExecuteRepository;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.common.cluster.entity.ClusterNodeEntity;
import com.temporedata.guard.modules.common.cluster.entity.ClusterNodeEntity;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.common.monitor.entity.MonitorEntity;
import com.temporedata.guard.modules.common.monitor.repository.MonitorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * System monitoring service.
 * Collects CPU, memory, disk, network metrics and provides system-level stats.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitorService {

    private final MonitorRepository monitorRepository;
    private final ClusterRepository clusterRepository;
    private final ClusterNodeRepository clusterNodeRepository;
    private final DatasourceRepository datasourceRepository;
    private final WorkflowRepository workflowRepository;
    private final WorkflowExecuteRepository workflowExecuteRepository;

    // ==================== System Monitor ====================

    public Map<String, Object> getSystemMonitor() {
        Map<String, Object> result = new HashMap<>();

        // Cluster stats
        long activeClusters = clusterRepository.countByStatus("ONLINE");
        long totalClusters = clusterRepository.count();
        result.put("cluster", Map.of("total", totalClusters, "active", activeClusters));

        // Datasource stats
        long totalDatasources = datasourceRepository.count();
        result.put("datasource", Map.of("total", totalDatasources, "active", totalDatasources));

        // Workflow stats
        long totalWorkflows = workflowRepository.count();
        long activeWorkflows = workflowRepository.countByStatus("ONLINE");
        result.put("workflow", Map.of("total", totalWorkflows, "active", activeWorkflows));

        // Execution stats
        long totalExecutions = workflowExecuteRepository.count();
        long successExecutions = workflowExecuteRepository.countByStatus("SUCCESS");
        result.put("execution", Map.of("total", totalExecutions, "success", successExecutions));

        return result;
    }

    // ==================== Cluster Monitor ====================

    public List<Map<String, Object>> getClusterMonitor(String clusterId, String timeType) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = getStartTime(end, timeType);

        List<MonitorEntity> records = monitorRepository.findByClusterIdAndTimeRange(clusterId, start, end);

        return records.stream().map(r -> {
            Map<String, Object> m = new HashMap<>();
            m.put("time", r.getCreateDateTime());
            m.put("cpuPercent", r.getCpuPercent());
            m.put("usedMemorySize", r.getUsedMemorySize());
            m.put("usedStorageSize", r.getUsedStorageSize());
            m.put("networkIoReadSpeed", r.getNetworkIoReadSpeed());
            m.put("networkIoWriteSpeed", r.getNetworkIoWriteSpeed());
            m.put("diskIoReadSpeed", r.getDiskIoReadSpeed());
            m.put("diskIoWriteSpeed", r.getDiskIoWriteSpeed());
            return m;
        }).collect(Collectors.toList());
    }

    // ==================== Scheduled Collection ====================

    /**
     * Scheduled task: collect node metrics every 60 seconds.
     */
    @Scheduled(cron = "0 * * * * ?")
    public void scheduleCollectMetrics() {
        List<ClusterNodeEntity> nodes = clusterNodeRepository.findAll();
        for (ClusterNodeEntity node : nodes) {
            try {
                collectNodeMetrics(node);
            } catch (Exception e) {
                log.error("Failed to collect metrics for node: {}", node.getId(), e);
            }
        }
        // Clean old records (keep 30 days)
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        monitorRepository.deleteByCreateDateTimeBefore(threshold);
    }

    private void collectNodeMetrics(ClusterNodeEntity node) {
        MonitorEntity monitor = new MonitorEntity();
        monitor.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        monitor.setClusterId(node.getClusterId());
        monitor.setClusterNodeId(node.getId());
        monitor.setTenantId("DEFAULT");
        monitor.setCreateDateTime(LocalDateTime.now());

        try {
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double cpuLoad = osBean.getSystemLoadAverage();
            monitor.setCpuPercent(cpuLoad >= 0 ? cpuLoad : 0);
            Runtime runtime = Runtime.getRuntime();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            monitor.setUsedMemorySize((totalMemory - freeMemory) / (1024.0 * 1024 * 1024));

            // Disk space (simplified)
            java.io.File root = new java.io.File("/");
            monitor.setUsedStorageSize((root.getTotalSpace() - root.getFreeSpace()) / (1024.0 * 1024 * 1024));

            monitor.setStatus("SUCCESS");
            monitor.setLog("OK");
        } catch (Exception e) {
            monitor.setStatus("FAIL");
            monitor.setLog(e.getMessage());
        }

        monitorRepository.save(monitor);
    }

    // ==================== Instance Monitor ====================

    public Map<String, Object> getInstanceMonitor() {
        Map<String, Object> result = new HashMap<>();

        long totalInstances = workflowExecuteRepository.count();
        long runningInstances = workflowExecuteRepository.countByStatus("RUNNING");
        long successInstances = workflowExecuteRepository.countByStatus("SUCCESS");
        long failedInstances = workflowExecuteRepository.countByStatus("FAIL");

        result.put("total", totalInstances);
        result.put("running", runningInstances);
        result.put("success", successInstances);
        result.put("failed", failedInstances);

        return result;
    }

    // ==================== Helper ====================

    private LocalDateTime getStartTime(LocalDateTime end, String timeType) {
        switch (timeType) {
            case "30_MIN": return end.minusMinutes(30);
            case "1_HOUR": return end.minusHours(1);
            case "6_HOUR": return end.minusHours(6);
            case "12_HOUR": return end.minusHours(12);
            case "1_DAY": return end.minusDays(1);
            case "7_DAY": return end.minusDays(7);
            case "30_DAY": return end.minusDays(30);
            default: return end.minusHours(1);
        }
    }
}