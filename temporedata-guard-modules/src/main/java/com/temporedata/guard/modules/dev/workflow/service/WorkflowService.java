package com.temporedata.guard.modules.dev.workflow.service;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEdgeEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowEdgeRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEntity;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowExecuteEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowExecuteRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeEntity;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeExecuteEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowNodeExecuteRepository;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowNodeRepository;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.workflow.*;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.workflow.entity.*;
import com.temporedata.guard.modules.dev.workflow.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowExecuteRepository executeRepository;
    private final WorkflowNodeExecuteRepository nodeExecuteRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final WorkflowDagService dagService;
    private final WorkflowExecutor workflowExecutor;
    private final QuartzScheduleService quartzScheduleService;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    // ---- CRUD ----

    public List<WorkflowRes> list(String tenantId) {
        return workflowRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::buildRes)
                .collect(Collectors.toList());
    }

    public WorkflowRes detail(String id) {
        return workflowRepository.findById(id)
                .map(this::buildRes)
                .orElseThrow(() -> new BusinessException("工作流不存在"));
    }

    @Transactional
    public WorkflowRes create(WorkflowReq req, String tenantId) {
        WorkflowEntity entity = new WorkflowEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setStatus("OFFLINE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        workflowRepository.save(entity);
        saveNodesAndEdges(entity.getId(), req.getNodes(), req.getEdges());
        return buildRes(entity);
    }

    @Transactional
    public WorkflowRes update(String id, WorkflowReq req) {
        WorkflowEntity entity = workflowRepository.findById(id)
                .orElseThrow(() -> new BusinessException("工作流不存在"));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setUpdateTime(LocalDateTime.now());
        workflowRepository.save(entity);
        nodeRepository.deleteByWorkflowId(id);
        edgeRepository.deleteByWorkflowId(id);
        saveNodesAndEdges(id, req.getNodes(), req.getEdges());
        return buildRes(entity);
    }

    @Transactional
    public void delete(String id) {
        quartzScheduleService.removeSchedule(id);
        nodeRepository.deleteByWorkflowId(id);
        edgeRepository.deleteByWorkflowId(id);
        workflowRepository.deleteById(id);
    }

    // ---- Schedule ----

    @Transactional
    public void updateSchedule(String workflowId, String cronExpression) {
        WorkflowEntity entity = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException("工作流不存在"));
        entity.setCronExpression(cronExpression);
        entity.setStatus("ONLINE");
        entity.setUpdateTime(LocalDateTime.now());
        workflowRepository.save(entity);
        quartzScheduleService.scheduleWorkflow(workflowId, cronExpression);
    }

    @Transactional
    public void disableSchedule(String workflowId) {
        WorkflowEntity entity = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException("工作流不存在"));
        quartzScheduleService.removeSchedule(workflowId);
        entity.setStatus("OFFLINE");
        entity.setCronExpression(null);
        entity.setUpdateTime(LocalDateTime.now());
        workflowRepository.save(entity);
    }

    // ---- Execution ----

    @Transactional
    public WorkflowExecuteEntity createExecution(String workflowId) {
        WorkflowExecuteEntity execution = new WorkflowExecuteEntity();
        execution.setId(UUID.randomUUID().toString().replace("-", ""));
        execution.setWorkflowId(workflowId);
        execution.setStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        execution.setLog("");
        return executeRepository.save(execution);
    }

    @Transactional
    public WorkflowExecuteRes execute(String workflowId) {
        WorkflowEntity workflow = workflowRepository.findById(workflowId)
                .orElseThrow(() -> new BusinessException("工作流不存在"));

        WorkflowExecuteEntity exec = new WorkflowExecuteEntity();
        exec.setId(UUID.randomUUID().toString().replace("-", ""));
        exec.setWorkflowId(workflowId);
        exec.setStatus("RUNNING");
        exec.setStartTime(LocalDateTime.now());
        exec.setLog("");
        executeRepository.save(exec);

        List<WorkflowNodeEntity> nodes = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
        List<WorkflowEdgeEntity> edges = edgeRepository.findByWorkflowId(workflowId);

        try {
            List<WorkflowNodeEntity> sorted = topologicalSort(nodes, edges);
            List<WorkflowExecuteRes.NodeExecuteItem> nodeResults = new ArrayList<>();
            boolean allSuccess = true;
            StringBuilder logBuf = new StringBuilder();

            for (WorkflowNodeEntity node : sorted) {
                logBuf.append("[").append(LocalDateTime.now()).append("] 执行节点: ").append(node.getName()).append("\n");
                log.info("Workflow [{}] executing node: {}", workflow.getName(), node.getName());

                WorkflowNodeExecuteEntity ne = new WorkflowNodeExecuteEntity();
                ne.setId(UUID.randomUUID().toString().replace("-", ""));
                ne.setExecuteId(exec.getId());
                ne.setNodeId(node.getId());
                ne.setNodeName(node.getName());
                ne.setStartTime(LocalDateTime.now());

                long start = System.currentTimeMillis();
                try {
                    executeNode(node);
                    ne.setStatus("SUCCESS");
                    ne.setDurationMs(System.currentTimeMillis() - start);
                    logBuf.append("  -> 成功 (耗时 ").append(ne.getDurationMs()).append("ms)\n");
                } catch (Exception e) {
                    ne.setStatus("FAILED");
                    ne.setDurationMs(System.currentTimeMillis() - start);
                    ne.setErrorMsg(e.getMessage());
                    logBuf.append("  -> 失败: ").append(e.getMessage()).append("\n");
                    allSuccess = false;
                }
                ne.setEndTime(LocalDateTime.now());
                nodeExecuteRepository.save(ne);

                nodeResults.add(toNodeItem(ne));
            }

            exec.setStatus(allSuccess ? "SUCCESS" : "FAILED");
            exec.setEndTime(LocalDateTime.now());
            exec.setLog(logBuf.toString());
            executeRepository.save(exec);

            WorkflowExecuteRes res = new WorkflowExecuteRes();
            res.setExecuteId(exec.getId());
            res.setStatus(exec.getStatus());
            res.setNodeResults(nodeResults);
            res.setStartTime(exec.getStartTime().toString());
            res.setEndTime(exec.getEndTime().toString());
            return res;
        } catch (Exception e) {
            exec.setStatus("FAILED");
            exec.setEndTime(LocalDateTime.now());
            exec.setLog("DAG execution error: " + e.getMessage());
            executeRepository.save(exec);
            throw new BusinessException("工作流执行失败: " + e.getMessage());
        }
    }

    public List<WorkflowExecuteRes> executionHistory(String workflowId) {
        return executeRepository.findByWorkflowIdOrderByStartTimeDesc(workflowId).stream()
                .map(e -> {
                    List<WorkflowNodeExecuteEntity> nodeExecs = nodeExecuteRepository.findByExecuteId(e.getId());
                    WorkflowExecuteRes res = new WorkflowExecuteRes();
                    res.setExecuteId(e.getId());
                    res.setStatus(e.getStatus());
                    res.setNodeResults(nodeExecs.stream().map(this::toNodeItem).collect(Collectors.toList()));
                    res.setStartTime(e.getStartTime().toString());
                    res.setEndTime(e.getEndTime() != null ? e.getEndTime().toString() : null);
                    return res;
                })
                .collect(Collectors.toList());
    }

    // ---- Bootstrap: re-register all active schedules ----

    public void registerAllSchedules() {
        quartzScheduleService.registerAllSchedules();
    }

    // ---- Internal ----

    private void saveNodesAndEdges(String workflowId, List<WorkflowNodeReq> nodes, List<WorkflowEdgeReq> edges) {
        if (nodes != null) {
            for (int i = 0; i < nodes.size(); i++) {
                WorkflowNodeReq n = nodes.get(i);
                WorkflowNodeEntity entity = new WorkflowNodeEntity();
                entity.setId(n.getId() != null ? n.getId() : UUID.randomUUID().toString().replace("-", ""));
                entity.setWorkflowId(workflowId);
                entity.setName(n.getName());
                entity.setType(n.getType() != null ? n.getType() : "SQL");
                entity.setDatasourceId(n.getDatasourceId());
                entity.setSql(n.getSql());
                entity.setPositionX(n.getPositionX());
                entity.setPositionY(n.getPositionY());
                entity.setSortOrder(i);
                entity.setRetryCount(n.getRetryCount() != null ? n.getRetryCount() : 0);
                entity.setRetryInterval(n.getRetryInterval() != null ? n.getRetryInterval() : 1);
                entity.setTimeoutSeconds(n.getTimeoutSeconds());
                entity.setPriority(n.getPriority() != null ? n.getPriority() : "MEDIUM");
                entity.setFailStrategy(n.getFailStrategy() != null ? n.getFailStrategy() : "STOP");
                entity.setParams(n.getParams());
                entity.setSparkConf(n.getSparkConf());
                entity.setHttpUrl(n.getHttpUrl());
                entity.setHttpMethod(n.getHttpMethod());
                entity.setHttpHeaders(n.getHttpHeaders());
                entity.setDependencyType(n.getDependencyType());
                entity.setDependencyTimeout(n.getDependencyTimeout());
                entity.setEmailTo(n.getEmailTo());
                entity.setEmailSubject(n.getEmailSubject());
                entity.setQualityType(n.getQualityType());
                entity.setQualityThreshold(n.getQualityThreshold());
                entity.setScopeSql(n.getScopeSql());
                nodeRepository.save(entity);
            }
        }
        if (edges != null) {
            for (WorkflowEdgeReq e : edges) {
                WorkflowEdgeEntity entity = new WorkflowEdgeEntity();
                entity.setId(e.getId() != null ? e.getId() : UUID.randomUUID().toString().replace("-", ""));
                entity.setWorkflowId(workflowId);
                entity.setSourceNodeId(e.getSourceNodeId());
                entity.setTargetNodeId(e.getTargetNodeId());
                entity.setEdgeType(e.getEdgeType() != null ? e.getEdgeType() : "SUCCESS");
                entity.setConditionExpr(e.getConditionExpr());
                edgeRepository.save(entity);
            }
        }
    }

    private List<WorkflowNodeEntity> topologicalSort(List<WorkflowNodeEntity> nodes, List<WorkflowEdgeEntity> edges) {
        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adj = new HashMap<>();
        for (WorkflowNodeEntity n : nodes) {
            inDegree.put(n.getId(), 0);
            adj.put(n.getId(), new ArrayList<>());
        }
        for (WorkflowEdgeEntity e : edges) {
            adj.get(e.getSourceNodeId()).add(e.getTargetNodeId());
            inDegree.merge(e.getTargetNodeId(), 1, Integer::sum);
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        Map<String, WorkflowNodeEntity> nodeMap = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeEntity::getId, n -> n));
        List<WorkflowNodeEntity> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            sorted.add(nodeMap.get(id));
            for (String next : adj.get(id)) {
                int d = inDegree.merge(next, -1, Integer::sum);
                if (d == 0) queue.add(next);
            }
        }
        if (sorted.size() != nodes.size()) {
            throw new BusinessException("DAG 存在环路，无法执行");
        }
        return sorted;
    }

    private void executeNode(WorkflowNodeEntity node) throws Exception {
        if (!"SQL".equals(node.getType())) return;
        DatasourceEntity ds = datasourceRepository.findById(node.getDatasourceId())
                .orElseThrow(() -> new BusinessException("数据源不存在: " + node.getDatasourceId()));
        String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        DataSource pool = connectionManager.getOrCreate(ds, plainPwd);
        try (Connection conn = pool.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(node.getSql());
        }
    }

    private WorkflowRes buildRes(WorkflowEntity e) {
        WorkflowRes res = new WorkflowRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setDescription(e.getDescription());
        res.setStatus(e.getStatus());
        res.setCronExpression(e.getCronExpression());
        res.setCreateTime(e.getCreateTime().toString());

        List<WorkflowNodeEntity> nodes = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(e.getId());
        Map<String, String> dsNameMap = new HashMap<>();
        for (WorkflowNodeEntity n : nodes) {
            if (n.getDatasourceId() != null) {
                dsNameMap.computeIfAbsent(n.getDatasourceId(), id ->
                        datasourceRepository.findById(id).map(DatasourceEntity::getName).orElse(""));
            }
        }
        res.setNodes(nodes.stream().map(n -> {
            WorkflowNodeRes nr = new WorkflowNodeRes();
            nr.setId(n.getId());
            nr.setName(n.getName());
            nr.setType(n.getType());
            nr.setDatasourceId(n.getDatasourceId());
            nr.setDatasourceName(dsNameMap.get(n.getDatasourceId()));
            nr.setSql(n.getSql());
            nr.setPositionX(n.getPositionX());
            nr.setPositionY(n.getPositionY());
            nr.setRetryCount(n.getRetryCount());
            nr.setRetryInterval(n.getRetryInterval());
            nr.setTimeoutSeconds(n.getTimeoutSeconds());
            nr.setPriority(n.getPriority());
            nr.setFailStrategy(n.getFailStrategy());
            nr.setParams(n.getParams());
            nr.setSparkConf(n.getSparkConf());
            nr.setHttpUrl(n.getHttpUrl());
            nr.setHttpMethod(n.getHttpMethod());
            nr.setHttpHeaders(n.getHttpHeaders());
            nr.setDependencyType(n.getDependencyType());
            nr.setDependencyTimeout(n.getDependencyTimeout());
            nr.setEmailTo(n.getEmailTo());
            nr.setEmailSubject(n.getEmailSubject());
            nr.setQualityType(n.getQualityType());
            nr.setQualityThreshold(n.getQualityThreshold());
            nr.setScopeSql(n.getScopeSql());
            return nr;
        }).collect(Collectors.toList()));

        res.setEdges(edgeRepository.findByWorkflowId(e.getId()).stream().map(edge -> {
            WorkflowEdgeRes er = new WorkflowEdgeRes();
            er.setId(edge.getId());
            er.setSourceNodeId(edge.getSourceNodeId());
            er.setTargetNodeId(edge.getTargetNodeId());
            er.setEdgeType(edge.getEdgeType());
            er.setConditionExpr(edge.getConditionExpr());
            return er;
        }).collect(Collectors.toList()));

        return res;
    }

    private WorkflowExecuteRes.NodeExecuteItem toNodeItem(WorkflowNodeExecuteEntity ne) {
        WorkflowExecuteRes.NodeExecuteItem item = new WorkflowExecuteRes.NodeExecuteItem();
        item.setNodeId(ne.getNodeId());
        item.setNodeName(ne.getNodeName());
        item.setStatus(ne.getStatus());
        item.setErrorMsg(ne.getErrorMsg());
        item.setDurationMs(ne.getDurationMs());
        return item;
    }

    /**
     * Convert WorkflowExecuteEntity to WorkflowExecuteRes (public for controller use).
     */
    public WorkflowExecuteRes toExecuteRes(WorkflowExecuteEntity exec) {
        List<WorkflowNodeExecuteEntity> nodeExecs = nodeExecuteRepository.findByExecuteId(exec.getId());
        WorkflowExecuteRes res = new WorkflowExecuteRes();
        res.setExecuteId(exec.getId());
        res.setStatus(exec.getStatus());
        res.setNodeResults(nodeExecs.stream().map(this::toNodeItem).collect(Collectors.toList()));
        res.setStartTime(exec.getStartTime() != null ? exec.getStartTime().toString() : null);
        res.setEndTime(exec.getEndTime() != null ? exec.getEndTime().toString() : null);
        return res;
    }
}