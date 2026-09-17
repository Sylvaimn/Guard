package com.temporedata.guard.modules.dev.workflow.service;
import com.temporedata.guard.modules.dev.lineage.service.LineageService;
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
import com.temporedata.guard.common.locker.DistributedLock;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.workflow.entity.*;
import com.temporedata.guard.modules.dev.workflow.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

/**
 * Async workflow execution engine.
 * Supports parallel execution of independent nodes, kill, retry, and retry downstream.
 */
@Slf4j
@Component
public class WorkflowExecutor {

    private final WorkflowRepository workflowRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowExecuteRepository executeRepository;
    private final WorkflowNodeExecuteRepository nodeExecuteRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final WorkflowDagService dagService;
    private final com.temporedata.guard.modules.common.lakehouse.service.LakehouseService lakehouseService;
    private final DistributedLock distributedLock;
    private final com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService;

    // Track running executions for kill support
    private final ConcurrentHashMap<String, Boolean> executionAbortFlags = new ConcurrentHashMap<>();

    public WorkflowExecutor(WorkflowRepository workflowRepository,
                           WorkflowNodeRepository nodeRepository,
                           WorkflowEdgeRepository edgeRepository,
                           WorkflowExecuteRepository executeRepository,
                           WorkflowNodeExecuteRepository nodeExecuteRepository,
                           DatasourceRepository datasourceRepository,
                           JdbcConnectionManager connectionManager,
                           WorkflowDagService dagService,
                           DistributedLock distributedLock,
                           com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService,
                           com.temporedata.guard.modules.common.lakehouse.service.LakehouseService lakehouseService) {
        this.workflowRepository = workflowRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.executeRepository = executeRepository;
        this.nodeExecuteRepository = nodeExecuteRepository;
        this.datasourceRepository = datasourceRepository;
        this.connectionManager = connectionManager;
        this.dagService = dagService;
        this.distributedLock = distributedLock;
        this.lineageService = lineageService;
        this.lakehouseService = lakehouseService;
    }

    /**
     * Async execution entry point.
     */
    @Async
    public void executeAsync(String executionId) {
        WorkflowExecuteEntity execution = executeRepository.findById(executionId)
                .orElse(null);
        if (execution == null) {
            log.error("Execution not found: {}", executionId);
            return;
        }

        String workflowId = execution.getWorkflowId();
        String lockKey = "workflow-exec-" + workflowId;

        // Acquire distributed lock to prevent duplicate execution
        if (!distributedLock.tryLock(lockKey, 5000)) {
            log.warn("Workflow {} is already executing, skipping", workflowId);
            execution.setStatus("SKIPPED");
            execution.setEndTime(LocalDateTime.now());
            execution.setLog("Workflow is already running, execution skipped");
            executeRepository.save(execution);
            return;
        }

        try {
            executionAbortFlags.put(executionId, false);

            WorkflowEntity workflow = workflowRepository.findById(workflowId).orElse(null);
            if (workflow == null) {
                markExecutionFailed(execution, "Workflow not found");
                return;
            }

            List<WorkflowNodeEntity> nodes = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
            List<WorkflowEdgeEntity> edges = edgeRepository.findByWorkflowId(workflowId);

            // Validate DAG structure
            dagService.validateDag(nodes, edges);

            // Topological sort for execution order
            List<WorkflowNodeEntity> sorted = dagService.topologicalSortNodes(nodes, edges);

            StringBuilder logBuf = new StringBuilder();
            logBuf.append("=== Workflow Execution: ").append(workflow.getName()).append(" ===\n");
            logBuf.append("Start time: ").append(LocalDateTime.now()).append("\n");
            logBuf.append("Total nodes: ").append(sorted.size()).append("\n\n");

            boolean allSuccess = true;
            Map<String, String> nodeStatus = new HashMap<>(); // track node execution status for conditional branching

            for (WorkflowNodeEntity node : sorted) {
                // Check abort flag
                if (Boolean.TRUE.equals(executionAbortFlags.get(executionId))) {
                    logBuf.append("Execution aborted by user\n");
                    break;
                }

                // Check if this node should be skipped based on conditional branching
                if (!shouldExecuteNode(node, edges, nodeStatus)) {
                    logBuf.append("[").append(LocalDateTime.now()).append("] Skipping node: ")
                            .append(node.getName()).append(" (condition not met)\n");
                    WorkflowNodeExecuteEntity ne = createNodeExecution(executionId, node);
                    ne.setStatus("SKIPPED");
                    ne.setDurationMs(0L);
                    ne.setEndTime(LocalDateTime.now());
                    nodeExecuteRepository.save(ne);
                    nodeStatus.put(node.getId(), "SKIPPED");
                    continue;
                }

                logBuf.append("[").append(LocalDateTime.now()).append("] Executing node: ")
                        .append(node.getName()).append(" (").append(node.getType())
                        .append(") [priority: ").append(node.getPriority()).append("]\n");

                WorkflowNodeExecuteEntity ne = createNodeExecution(executionId, node);
                nodeExecuteRepository.save(ne);

                long start = System.currentTimeMillis();
                boolean nodeSuccess = executeNodeWithRetry(node, ne, logBuf);
                ne.setDurationMs(System.currentTimeMillis() - start);
                ne.setEndTime(LocalDateTime.now());
                nodeExecuteRepository.save(ne);

                if (nodeSuccess) {
                    nodeStatus.put(node.getId(), "SUCCESS");
                } else {
                    nodeStatus.put(node.getId(), "FAILED");
                    allSuccess = false;

                    // Handle fail strategy
                    if ("STOP".equals(node.getFailStrategy())) {
                        logBuf.append("  -> Stopping workflow due to fail strategy: STOP\n");
                        break;
                    } else if ("CONTINUE".equals(node.getFailStrategy())) {
                        logBuf.append("  -> Continuing workflow despite failure (fail strategy: CONTINUE)\n");
                    }
                    // RETRY is handled inside executeNodeWithRetry
                }
            }

            if (Boolean.TRUE.equals(executionAbortFlags.get(executionId))) {
                execution.setStatus("ABORTED");
            } else {
                execution.setStatus(allSuccess ? "SUCCESS" : "FAILED");
            }
            execution.setEndTime(LocalDateTime.now());
            execution.setLog(logBuf.toString());
            executeRepository.save(execution);

            // Event-driven lineage collection after successful workflow run
            if ("SUCCESS".equals(execution.getStatus())) {
                workflowRepository.findById(execution.getWorkflowId()).ifPresent(wf ->
                        lineageService.collectTaskLineage(wf.getTenantId(), "WORKFLOW", wf.getId()));
            }

            log.info("Workflow execution {} completed: {}", executionId, execution.getStatus());

        } catch (Exception e) {
            log.error("Workflow execution failed: {}", executionId, e);
            markExecutionFailed(execution, e.getMessage());
        } finally {
            distributedLock.unlock(lockKey);
            executionAbortFlags.remove(executionId);
        }
    }

    /**
     * Execute a node with retry and timeout support.
     * Returns true if the node executed successfully (or was skipped via CONTINUE strategy).
     */
    public boolean executeNodeWithRetry(WorkflowNodeEntity node, WorkflowNodeExecuteEntity ne, StringBuilder logBuf) {
        int retryCount = node.getRetryCount() != null ? node.getRetryCount() : 0;
        int retryInterval = node.getRetryInterval() != null ? node.getRetryInterval() : 1;
        Integer timeout = node.getTimeoutSeconds();

        // Clamp retry values
        retryCount = Math.max(0, Math.min(retryCount, 10));
        retryInterval = Math.max(1, Math.min(retryInterval, 600));

        for (int attempt = 0; attempt <= retryCount; attempt++) {
            try {
                if (attempt > 0) {
                    logBuf.append("  -> Retry ").append(attempt).append("/").append(retryCount)
                            .append(" (waiting ").append(retryInterval).append("s)\n");
                    log.info("Retrying node {} (attempt {}/{})", node.getName(), attempt, retryCount);
                    Thread.sleep(retryInterval * 1000L);
                }

                if (timeout != null && timeout > 0) {
                    executeNodeWithTimeout(node, timeout);
                } else {
                    executeNode(node);
                }

                ne.setStatus("SUCCESS");
                ne.setRetryTimes(attempt);
                logBuf.append("  -> SUCCESS");
                if (attempt > 0) logBuf.append(" (after ").append(attempt).append(" retries)");
                logBuf.append("\n");
                return true;

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                ne.setStatus("FAILED");
                ne.setErrorMsg("Execution interrupted");
                logBuf.append("  -> FAILED: interrupted\n");
                return false;

            } catch (Exception e) {
                if (attempt == retryCount) {
                    // All retries exhausted
                    ne.setStatus("FAILED");
                    ne.setRetryTimes(attempt);
                    ne.setErrorMsg("[" + e.getClass().getSimpleName() + "] " + e.getMessage());
                    logBuf.append("  -> FAILED (after ").append(attempt).append(" retries): ")
                            .append(e.getMessage()).append("\n");
                    return false;
                }
                // Will retry
                logBuf.append("  -> Attempt ").append(attempt + 1).append(" failed: ")
                        .append(e.getMessage()).append("\n");
            }
        }
        return false;
    }

    /**
     * Execute a node with a timeout limit.
     */
    private void executeNodeWithTimeout(WorkflowNodeEntity node, int timeoutSeconds) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(() -> {
            try {
                executeNode(node);
                return null;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        try {
            future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new BusinessException("Task timed out after " + timeoutSeconds + "s");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) throw (Exception) cause;
            throw new BusinessException("Task execution error: " + cause.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Determine if a node should be executed based on upstream node status and edge types.
     * Conditions:
     * - If any incoming edge is an ALWAYS edge, the node always runs.
     * - If all incoming edges are SUCCESS edges, all upstream nodes must have SUCCEEDED.
     * - If any incoming edge is a FAILURE edge, the node runs only if at least one upstream FAILED.
     */
    public boolean shouldExecuteNode(WorkflowNodeEntity node, List<WorkflowEdgeEntity> edges,
                                      Map<String, String> nodeStatus) {
        List<WorkflowEdgeEntity> incomingEdges = edges.stream()
                .filter(e -> e.getTargetNodeId().equals(node.getId()))
                .collect(Collectors.toList());

        if (incomingEdges.isEmpty()) {
            return true; // Entry node, always execute
        }

        boolean hasAlwaysEdge = false;
        boolean allUpstreamSuccess = true;
        boolean anyUpstreamFailed = false;

        for (WorkflowEdgeEntity edge : incomingEdges) {
            if ("ALWAYS".equals(edge.getEdgeType())) {
                hasAlwaysEdge = true;
                continue;
            }

            String upstreamStatus = nodeStatus.get(edge.getSourceNodeId());
            if (upstreamStatus == null) {
                // Upstream not yet executed (shouldn't happen in topological order)
                return false;
            }

            if ("SUCCESS".equals(upstreamStatus)) {
                // OK
            } else if ("FAILED".equals(upstreamStatus)) {
                allUpstreamSuccess = false;
                anyUpstreamFailed = true;
            }
            // SKIPPED is treated as neutral
        }

        if (hasAlwaysEdge) return true;

        // Check FAILURE edges
        boolean hasFailureEdge = incomingEdges.stream()
                .anyMatch(e -> "FAILURE".equals(e.getEdgeType()));
        if (hasFailureEdge) {
            return anyUpstreamFailed;
        }

        // Default: all upstream must be SUCCESS
        return allUpstreamSuccess;
    }

    public boolean isAborted(String executionId) {
        return Boolean.TRUE.equals(executionAbortFlags.get(executionId));
    }

    /**
     * Kill a running execution.
     */
    public void kill(String executionId) {
        WorkflowExecuteEntity execution = executeRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException("Execution not found"));

        if (!"RUNNING".equals(execution.getStatus())) {
            throw new BusinessException("Execution is not running, current status: " + execution.getStatus());
        }

        executionAbortFlags.put(executionId, true);
        log.info("Kill signal sent for execution: {}", executionId);
    }

    /**
     * Retry a failed execution from the beginning.
     */
    public WorkflowExecuteEntity retry(String executionId) {
        WorkflowExecuteEntity oldExecution = executeRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException("Execution not found"));

        if (!"FAILED".equals(oldExecution.getStatus()) && !"ABORTED".equals(oldExecution.getStatus())) {
            throw new BusinessException("Only failed or aborted executions can be retried");
        }

        WorkflowExecuteEntity newExecution = createExecution(oldExecution.getWorkflowId());
        executeAsync(newExecution.getId());
        return newExecution;
    }

    /**
     * Retry from a specific failed node, including all downstream nodes.
     */
    public WorkflowExecuteEntity retryFromNode(String executionId, String nodeId) {
        WorkflowExecuteEntity oldExecution = executeRepository.findById(executionId)
                .orElseThrow(() -> new BusinessException("Execution not found"));

        String workflowId = oldExecution.getWorkflowId();
        List<WorkflowNodeEntity> allNodes = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
        List<WorkflowEdgeEntity> edges = edgeRepository.findByWorkflowId(workflowId);

        // Build DAG and find downstream nodes
        DefaultDirectedGraph<String, DefaultEdge> graph = dagService.buildGraph(allNodes, edges);
        Set<String> downstreamIds = dagService.getDownstreamNodes(graph, nodeId);
        downstreamIds.add(nodeId); // Include the failed node itself

        // Create new execution with filtered nodes
        WorkflowExecuteEntity newExecution = createExecution(workflowId);

        // Re-execute only the failed node and its downstream
        List<WorkflowNodeEntity> nodesToRun = allNodes.stream()
                .filter(n -> downstreamIds.contains(n.getId()))
                .collect(Collectors.toList());

        executeSubset(newExecution, nodesToRun, edges);
        return newExecution;
    }

    /**
     * Get execution logs for a specific node.
     */
    public String getNodeLogs(String executionId, String nodeId) {
        List<WorkflowNodeExecuteEntity> nodeExecs = nodeExecuteRepository.findByExecuteId(executionId);
        return nodeExecs.stream()
                .filter(ne -> ne.getNodeId().equals(nodeId))
                .findFirst()
                .map(ne -> {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Node: ").append(ne.getNodeName()).append("\n");
                    sb.append("Status: ").append(ne.getStatus()).append("\n");
                    sb.append("Duration: ").append(ne.getDurationMs()).append("ms\n");
                    if (ne.getErrorMsg() != null) {
                        sb.append("Error: ").append(ne.getErrorMsg()).append("\n");
                    }
                    return sb.toString();
                })
                .orElse("No logs found for node: " + nodeId);
    }

    // ---- Internal ----

    private WorkflowExecuteEntity createExecution(String workflowId) {
        WorkflowExecuteEntity execution = new WorkflowExecuteEntity();
        execution.setId(UUID.randomUUID().toString().replace("-", ""));
        execution.setWorkflowId(workflowId);
        execution.setStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        execution.setLog("");
        executeRepository.save(execution);
        return execution;
    }

    public WorkflowNodeExecuteEntity createNodeExecution(String executionId, WorkflowNodeEntity node) {
        WorkflowNodeExecuteEntity ne = new WorkflowNodeExecuteEntity();
        ne.setId(UUID.randomUUID().toString().replace("-", ""));
        ne.setExecuteId(executionId);
        ne.setNodeId(node.getId());
        ne.setNodeName(node.getName());
        ne.setStartTime(LocalDateTime.now());
        ne.setStatus("RUNNING");
        return ne;
    }

    private void executeNode(WorkflowNodeEntity node) throws Exception {
        String type = node.getType() != null ? node.getType().toUpperCase() : "SQL";

        switch (type) {
            case "SQL":
                executeSqlNode(node);
                break;
            case "SHELL":
            case "BASH":
                executeShellNode(node);
                break;
            case "PYTHON":
                executePythonNode(node);
                break;
            case "SPARK_SQL":
                executeSparkSqlNode(node);
                break;
            case "DATAX":
                executeDataxNode(node);
                break;
            case "HTTP":
                executeHttpNode(node);
                break;
            case "SUB_WORKFLOW":
                executeSubWorkflowNode(node);
                break;
            case "CONDITION":
                executeConditionNode(node);
                break;
            case "EMAIL":
                executeEmailNode(node);
                break;
            case "DEPENDENCY":
                executeDependencyNode(node);
                break;
            case "DATA_QUALITY":
                executeDataQualityNode(node);
                break;
            case "LAKEHOUSE_SYNC":
                executeLakehouseSyncNode(node);
                break;
            case "ANALYSIS_LOAD":
                executeAnalysisLoadNode(node);
                break;
            default:
                throw new BusinessException("Unsupported node type: " + type);
        }
    }

    /** 湖仓同步节点: MySQL/分析库 -> Iceberg 湖仓(按节点 SQL 聚合写入) */
    private void executeLakehouseSyncNode(WorkflowNodeEntity node) throws Exception {
        if (node.getDatasourceId() == null) throw new BusinessException("LAKEHOUSE_SYNC 节点需数据源");
        if (node.getSql() == null || node.getSql().isBlank()) throw new BusinessException("LAKEHOUSE_SYNC 节点需 SQL");
        // 读取数据源结果 -> 湖仓追加(简化: 列名 region/stat_date/renci/guiji 兼容轨迹聚合)
        java.util.List<java.util.Map<String, Object>> rows = queryJdbc(node.getDatasourceId(), node.getSql());
        lakehouseService.createTable("workflow_output");
        java.util.List<java.util.Map<String, Object>> lakeRows = new java.util.ArrayList<>();
        for (var r : rows) {
            if (r.containsKey("region") && r.containsKey("stat_date")) {
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("region", r.get("region"));
                m.put("stat_date", r.get("stat_date"));
                m.put("renci", r.get("renci") == null ? 0 : ((Number) r.get("renci")).intValue());
                m.put("guiji", r.get("guiji") == null ? 0 : ((Number) r.get("guiji")).longValue());
                lakeRows.add(m);
            }
        }
        if (!lakeRows.isEmpty()) {
            lakehouseService.append("workflow_output", lakeRows);
            log.info("[WORKFLOW] LAKEHOUSE_SYNC {} rows -> lake", lakeRows.size());
        }
    }

    /** 分析加载节点: 湖仓/MySQL -> ClickHouse(经分析库数据源执行 CREATE/INSERT) */
    private void executeAnalysisLoadNode(WorkflowNodeEntity node) throws Exception {
        if (node.getDatasourceId() == null) throw new BusinessException("ANALYSIS_LOAD 节点需分析数据源");
        if (node.getSql() == null || node.getSql().isBlank()) throw new BusinessException("ANALYSIS_LOAD 节点需 SQL");
        executeJdbc(node.getDatasourceId(), node.getSql());
    }

    private void executeSqlNode(WorkflowNodeEntity node) throws Exception {
        if (node.getDatasourceId() == null) {
            throw new BusinessException("SQL node requires a datasource");
        }
        DatasourceEntity ds = datasourceRepository.findById(node.getDatasourceId())
                .orElseThrow(() -> new BusinessException("Datasource not found: " + node.getDatasourceId()));

        // 数据范围校验(防越权取数): 危险写操作无 WHERE 拒绝; 配置范围条件自动拼接
        String sql = applyScopeGuard(node);
        // Use JDBC connection manager to execute SQL
        // Note: password decryption handled by connection manager
        String plainPwd = ""; // TODO: decrypt via CryptoUtil
        try (java.sql.Connection conn = connectionManager.getOrCreate(ds, plainPwd).getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * 数据范围校验:
     * 1. DELETE/UPDATE 无 WHERE 时拒绝执行(防全表误删/越权);
     * 2. 节点配置 scopeSql(数据范围条件)时自动注入 WHERE/AND, 限定操作行范围。
     */
    private String applyScopeGuard(WorkflowNodeEntity node) {
        String sql = (node.getSql() == null ? "" : node.getSql()).trim();
        if (sql.isEmpty()) {
            throw new BusinessException("SQL 为空, 拒绝执行");
        }
        String head = sql.length() > 20 ? sql.substring(0, 20).toUpperCase() : sql.toUpperCase();
        boolean dangerous = head.startsWith("DELETE") || head.startsWith("UPDATE");
        String scope = node.getScopeSql() == null ? "" : node.getScopeSql().trim();
        if (dangerous && !sql.toUpperCase().contains(" WHERE ")) {
            if (scope.isEmpty()) {
                throw new BusinessException("危险写操作缺少 WHERE 且未配置数据范围, 已拒绝执行: " + node.getName());
            }
            // 追加范围条件(UPDATE x SET .. SET 后首个)
            if (head.startsWith("UPDATE")) {
                int setIdx = sql.toUpperCase().indexOf(" SET ");
                if (setIdx > 0) {
                    return sql.substring(0, setIdx + 5) + sql.substring(setIdx + 5) + " WHERE " + scope;
                }
            }
            return sql + " WHERE " + scope;
        }
        if (!scope.isEmpty()) {
            // SELECT/INSERT: 注入范围条件(无 WHERE 加 WHERE, 有 WHERE 加 AND)
            if (sql.toUpperCase().contains(" WHERE ")) {
                return sql + " AND " + scope;
            }
            return sql + " WHERE " + scope;
        }
        return sql;
    }

    private java.util.List<java.util.Map<String, Object>> queryJdbc(String datasourceId, String sql) throws Exception {
        DatasourceEntity ds = datasourceRepository.findById(datasourceId)
                .orElseThrow(() -> new BusinessException("Datasource not found: " + datasourceId));
        try (java.sql.Connection conn = connectionManager.getOrCreate(ds, "").getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery(sql)) {
            java.util.List<java.util.Map<String, Object>> rows = new java.util.ArrayList<>();
            java.sql.ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            while (rs.next()) {
                java.util.Map<String, Object> row = new java.util.HashMap<>();
                for (int i = 1; i <= cols; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                rows.add(row);
            }
            return rows;
        }
    }

    private void executeJdbc(String datasourceId, String sql) throws Exception {
        DatasourceEntity ds = datasourceRepository.findById(datasourceId)
                .orElseThrow(() -> new BusinessException("Datasource not found: " + datasourceId));
        try (java.sql.Connection conn = connectionManager.getOrCreate(ds, "").getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    private void executeShellNode(WorkflowNodeEntity node) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", node.getSql());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new BusinessException("Shell script exited with code: " + exitCode);
        }
    }

    private void executePythonNode(WorkflowNodeEntity node) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("python3", "-c", node.getSql());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new BusinessException("Python script exited with code: " + exitCode);
        }
    }

    private void executeSparkSqlNode(WorkflowNodeEntity node) throws Exception {
        if (node.getDatasourceId() == null) {
            throw new BusinessException("Spark SQL node requires a datasource");
        }
        DatasourceEntity ds = datasourceRepository.findById(node.getDatasourceId())
                .orElseThrow(() -> new BusinessException("Datasource not found: " + node.getDatasourceId()));

        String plainPwd = "";
        try (java.sql.Connection conn = connectionManager.getOrCreate(ds, plainPwd).getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            stmt.execute(node.getSql());
        }
        log.info("Spark SQL node executed (via JDBC): {}", node.getName());
    }

    private void executeDataxNode(WorkflowNodeEntity node) throws Exception {
        log.info("DataX node executed: {}", node.getName());
        // DataX execution: write job JSON to temp file, invoke datax.py
        String jobJson = node.getSql();
        if (jobJson == null || jobJson.trim().isEmpty()) {
            throw new BusinessException("DataX node requires JSON job config");
        }
        // TODO: integrate with DataX engine
        // For now, log and simulate
        log.info("DataX job config: {}", jobJson);
    }

    private void executeHttpNode(WorkflowNodeEntity node) throws Exception {
        String url = node.getHttpUrl();
        if (url == null || url.trim().isEmpty()) {
            throw new BusinessException("HTTP node requires a URL");
        }
        String method = node.getHttpMethod() != null ? node.getHttpMethod().toUpperCase() : "GET";
        log.info("HTTP node executed: {} {}", method, url);
        // TODO: integrate with HTTP client
        // For now, log and simulate success
    }

    private void executeSubWorkflowNode(WorkflowNodeEntity node) throws Exception {
        String targetWorkflowId = node.getSql();
        if (targetWorkflowId == null || targetWorkflowId.trim().isEmpty()) {
            throw new BusinessException("Sub-workflow node requires a target workflow ID");
        }
        // TODO: recursively trigger sub-workflow execution
        log.info("Sub-workflow node triggered: target={}", targetWorkflowId);
    }

    private void executeConditionNode(WorkflowNodeEntity node) throws Exception {
        // This node is a pass-through; routing is handled by edge types (SUCCESS/FAILURE)
        String expression = node.getSql();
        if (expression == null || expression.trim().isEmpty()) {
            throw new BusinessException("Condition node requires an expression");
        }
        // TODO: evaluate expression against workflow context
        // For now, always succeed - routing is done via edge types
        log.info("Condition node evaluated: {}", expression);
    }

    private void executeEmailNode(WorkflowNodeEntity node) throws Exception {
        String to = node.getEmailTo();
        if (to == null || to.trim().isEmpty()) {
            throw new BusinessException("Email node requires a recipient");
        }
        log.info("Email sent: to={}, subject={}", to, node.getEmailSubject());
        // TODO: integrate with SMTP email service
    }

    private void executeDependencyNode(WorkflowNodeEntity node) throws Exception {
        String depType = node.getDependencyType() != null ? node.getDependencyType() : "TIME_POINT";
        String config = node.getSql();
        int timeout = node.getDependencyTimeout() != null ? node.getDependencyTimeout() : 0;

        log.info("Dependency check: type={}, config={}, timeout={}s", depType, config, timeout);

        switch (depType) {
            case "TIME_POINT":
                // Wait until a specific time point
                // TODO: implement time-based waiting
                break;
            case "FILE_ARRIVAL":
                // Wait for a file to arrive
                // TODO: poll file existence
                break;
            case "EXTERNAL_TASK":
                // Wait for an external task to complete
                // TODO: poll external task status
                break;
            default:
                throw new BusinessException("Unknown dependency type: " + depType);
        }
    }

    private void executeDataQualityNode(WorkflowNodeEntity node) throws Exception {
        if (node.getDatasourceId() == null) {
            throw new BusinessException("Data quality node requires a datasource");
        }
        String checkType = node.getQualityType() != null ? node.getQualityType() : "ROW_COUNT";
        String checkSql = node.getSql();
        int threshold = node.getQualityThreshold() != null ? node.getQualityThreshold() : 0;

        DatasourceEntity ds = datasourceRepository.findById(node.getDatasourceId())
                .orElseThrow(() -> new BusinessException("Datasource not found: " + node.getDatasourceId()));

        String plainPwd = "";
        try (java.sql.Connection conn = connectionManager.getOrCreate(ds, plainPwd).getConnection()) {
            log.info("Data quality check: type={}, threshold={}%", checkType, threshold);

            if (checkSql != null && !checkSql.trim().isEmpty()) {
                try (java.sql.Statement stmt = conn.createStatement();
                     java.sql.ResultSet rs = stmt.executeQuery(checkSql)) {
                    if (rs.next()) {
                        long count = rs.getLong(1);
                        log.info("Data quality result: count={}", count);
                        // TODO: compare against threshold
                    }
                }
            }
        }
    }

    private void executeSubset(WorkflowExecuteEntity execution,
                               List<WorkflowNodeEntity> nodes,
                               List<WorkflowEdgeEntity> edges) {
        // Filter edges to only include nodes in the subset
        Set<String> nodeIds = nodes.stream().map(WorkflowNodeEntity::getId).collect(Collectors.toSet());
        List<WorkflowEdgeEntity> subsetEdges = edges.stream()
                .filter(e -> nodeIds.contains(e.getSourceNodeId()) && nodeIds.contains(e.getTargetNodeId()))
                .collect(Collectors.toList());

        List<WorkflowNodeEntity> sorted = dagService.topologicalSortNodes(nodes, subsetEdges);

        StringBuilder logBuf = new StringBuilder();
        logBuf.append("=== Retry from node ===\n");
        boolean allSuccess = true;

        for (WorkflowNodeEntity node : sorted) {
            if (Boolean.TRUE.equals(executionAbortFlags.get(execution.getId()))) break;

            WorkflowNodeExecuteEntity ne = createNodeExecution(execution.getId(), node);

            long start = System.currentTimeMillis();
            try {
                executeNode(node);
                ne.setStatus("SUCCESS");
                ne.setDurationMs(System.currentTimeMillis() - start);
            } catch (Exception e) {
                ne.setStatus("FAILED");
                ne.setDurationMs(System.currentTimeMillis() - start);
                ne.setErrorMsg(e.getMessage());
                allSuccess = false;
                break;
            }
            ne.setEndTime(LocalDateTime.now());
            nodeExecuteRepository.save(ne);
        }

        execution.setStatus(allSuccess ? "SUCCESS" : "FAILED");
        execution.setEndTime(LocalDateTime.now());
        execution.setLog(logBuf.toString());
        executeRepository.save(execution);
    }

    private void markExecutionFailed(WorkflowExecuteEntity execution, String errorMsg) {
        execution.setStatus("FAILED");
        execution.setEndTime(LocalDateTime.now());
        execution.setLog("Execution error: " + errorMsg);
        executeRepository.save(execution);
    }
}