package com.temporedata.guard.modules.common.ha.service;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowExecuteEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowExecuteRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeExecuteEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowNodeExecuteRepository;

import com.temporedata.guard.modules.common.ha.entity.HaServerEntity;
import com.temporedata.guard.modules.common.ha.repository.HaServerRepository;
import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * High availability service.
 * Handles server registration, heartbeat, and job state recovery after restart.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HaService {

    private final HaServerRepository haServerRepository;
    private final WorkflowExecuteRepository workflowExecuteRepository;
    private final WorkflowNodeExecuteRepository workflowNodeExecuteRepository;
    private final SyncTaskRepository syncTaskRepository;

    @Value("${server.port:8080}")
    private int serverPort;

    private String serverId;

    /**
     * Register this server on application startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        registerServer();
        recoverRunningJobs();
    }

    @Transactional
    public void registerServer() {
        String host = getLocalHost();
        HaServerEntity existing = haServerRepository.findByHostAndPort(host, serverPort);
        if (existing != null) {
            serverId = existing.getId();
            existing.setStatus("ACTIVE");
            existing.setLastHeartbeat(LocalDateTime.now());
            haServerRepository.save(existing);
        } else {
            serverId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            HaServerEntity server = new HaServerEntity();
            server.setId(serverId);
            server.setServerName("zy-server-" + serverPort);
            server.setHost(host);
            server.setPort(serverPort);
            server.setStatus("ACTIVE");
            server.setLastHeartbeat(LocalDateTime.now());
            server.setRegisterTime(LocalDateTime.now());
            server.setTenantId("DEFAULT");
            haServerRepository.save(server);
        }
        log.info("HA server registered: {}:{}", host, serverPort);
    }

    /**
     * Heartbeat every 30 seconds.
     */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void heartbeat() {
        if (serverId != null) {
            haServerRepository.findById(serverId).ifPresent(server -> {
                server.setLastHeartbeat(LocalDateTime.now());
                haServerRepository.save(server);
            });
        }
        // Mark offline servers (no heartbeat for 90 seconds)
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(90);
        List<HaServerEntity> staleServers = haServerRepository.findByLastHeartbeatBefore(threshold);
        for (HaServerEntity s : staleServers) {
            if ("ACTIVE".equals(s.getStatus())) {
                s.setStatus("OFFLINE");
                haServerRepository.save(s);
                log.warn("Server marked offline: {}:{}", s.getHost(), s.getPort());
                // Recover jobs from offline server
                recoverJobsFromServer(s.getId());
            }
        }
    }

    /**
     * Recover running jobs that were interrupted by server restart.
     * - 工作流执行: 标记 FAIL（不可安全自动续跑，人工重跑）
     * - 同步任务: 标记 READY（幂等/可重跑，恢复调度后可自动续跑）
     */
    @Transactional
    public void recoverRunningJobs() {
        List<WorkflowExecuteEntity> runningExecutions = workflowExecuteRepository.findByStatus("RUNNING");

        for (WorkflowExecuteEntity execution : runningExecutions) {
            execution.setStatus("FAIL");
            execution.setEndTime(LocalDateTime.now());
            execution.setLog((execution.getLog() != null ? execution.getLog() : "")
                    + "\n[HA] Job recovered after server restart - marked as FAILED");
            workflowExecuteRepository.save(execution);
            log.info("Recovered workflow execution: {} -> FAILED", execution.getId());
        }

        // Also recover node executions
        List<WorkflowNodeExecuteEntity> runningNodes = workflowNodeExecuteRepository.findByStatus("RUNNING");
        for (WorkflowNodeExecuteEntity n : runningNodes) {
            n.setStatus("FAIL");
            n.setEndTime(LocalDateTime.now());
            workflowNodeExecuteRepository.save(n);
        }

        // 同步任务恢复: RUNNING -> READY（可重跑）
        List<SyncTaskEntity> runningSyncs = syncTaskRepository.findByStatus("RUNNING");
        for (SyncTaskEntity t : runningSyncs) {
            t.setStatus("READY");
            t.setUpdateTime(LocalDateTime.now());
            syncTaskRepository.save(t);
            log.info("Recovered sync task: {} -> READY (可重新执行)", t.getId());
        }

        log.info("HA recovery complete: {} workflow executions, {} node executions, {} sync tasks recovered",
                runningExecutions.size(), runningNodes.size(), runningSyncs.size());
    }

    @Transactional
    public void recoverJobsFromServer(String serverId) {
        // Recover jobs that were running on the offline server
        log.info("Recovering jobs from offline server: {}", serverId);
        recoverRunningJobs();
    }

    public List<HaServerEntity> getActiveServers() {
        return haServerRepository.findByStatus("ACTIVE");
    }

    public List<HaServerEntity> getAllServers() {
        return haServerRepository.findAll();
    }

    private String getLocalHost() {
        try {
            return java.net.InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}