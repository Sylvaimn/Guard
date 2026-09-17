package com.temporedata.guard.modules.dev.lineage.service;
import com.temporedata.guard.modules.dev.catalog.entity.ColumnMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.ColumnMetadataRepository;
import com.temporedata.guard.modules.dev.lineage.entity.LineageEdgeEntity;
import com.temporedata.guard.modules.dev.lineage.repository.LineageEdgeRepository;
import com.temporedata.guard.modules.dev.lineage.entity.LineageNodeEntity;
import com.temporedata.guard.modules.dev.lineage.repository.LineageNodeRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaFieldLineageEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaFieldLineageRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaLineageEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaLineageRepository;
import com.temporedata.guard.modules.dev.catalog.entity.TableMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEdgeEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowEdgeRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowNodeRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.lineage.CycleAnalysisRes;
import com.temporedata.guard.api.lineage.HotNodeRes;
import com.temporedata.guard.api.lineage.LineagePathRes;
import com.temporedata.guard.api.meta.ImpactAnalysisRes;
import com.temporedata.guard.api.meta.LineageGraphRes;
import com.temporedata.guard.api.meta.SqlLineageRes;

import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.ingestion.entity.IngestionTaskEntity;
import com.temporedata.guard.modules.dev.ingestion.repository.IngestionTaskRepository;
import com.temporedata.guard.modules.dev.lineage.ws.LineageWsHub;
import com.temporedata.guard.modules.dev.meta.service.SqlLineageParser;
import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;
import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Enterprise-grade lineage module (single-node implementation of the distributed design):
 * graph model nodes/edges, SQL-based collection, and analysis engine
 * (graph exploration, impact, lineage tracing, path, cycles, heat, search, export).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LineageService {

    private final LineageNodeRepository nodeRepository;
    private final LineageEdgeRepository edgeRepository;
    private final TableMetadataRepository tableMetadataRepository;
    private final ColumnMetadataRepository columnMetadataRepository;
    private final MetaLineageRepository metaLineageRepository;
    private final MetaFieldLineageRepository metaFieldLineageRepository;
    private final LineageCacheService cacheService;
    private final DatasourceRepository datasourceRepository;
    private final SyncTaskRepository syncTaskRepository;
    private final com.temporedata.guard.modules.common.resource.repository.ResourceRepository resourceRepository;
    private final IngestionTaskRepository ingestionTaskRepository;
    private final QualityRuleRepository qualityRuleRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowEdgeRepository workflowEdgeRepository;
    private final SqlLineageParser sqlLineageParser;

    // ==================== Collection ====================

    /**
     * Full rebuild of the tenant lineage graph (equivalent of design's full rebuild API).
     * Sources: metadata tables/columns, sync tasks, ingestion, quality rules, workflow DAG + SQL parse.
     */
    @Transactional
    public Map<String, Object> rebuild(String tenantId) {
        nodeRepository.deleteByTenantId(tenantId);
        edgeRepository.deleteByTenantId(tenantId);
        cacheService.clear(tenantId);

        long nodeCount = 0;
        long edgeCount = 0;

        // 1. Metadata tables -> TABLE nodes, columns -> COLUMN nodes + INCLUDES edges
        List<TableMetadataEntity> tables = tableMetadataRepository.findByTenantIdOrderBySyncTimeDesc(tenantId);
        for (TableMetadataEntity t : tables) {
            String tableId = saveTableNode(t, tenantId);
            nodeCount++;
            List<ColumnMetadataEntity> columns = columnMetadataRepository.findByTableMetadataId(t.getId());
            for (ColumnMetadataEntity c : columns) {
                saveColumnNode(tableId, t.getTableName(), c, tenantId);
                nodeCount++;
                saveEdge(tableId, columnId(tableId, c.getColumnName()), "INCLUDES", null, null,
                        null, null, "表包含字段", tenantId);
                edgeCount++;
            }
        }

        // 2. Sync tasks: TASK node + FLOWS_TO + CONSUMES + PRODUCES
        for (SyncTaskEntity st : syncTaskRepository.findAll()) {
            String sourceTable = normalizeTableName(st.getSourceTable());
            String targetTable = normalizeTableName(st.getTargetTable());
            String sourceTableId = tableNodeId(sourceTable);
            String targetTableId = tableNodeId(targetTable);
            ensureTableNode(sourceTable, null, tenantId);
            ensureTableNode(targetTable, null, tenantId);
            nodeCount += 2;

            String taskId = taskNodeId("SYNC", st.getName(), tenantId);
            saveTaskNode(taskId, "SYNC", st.getName(), tenantId);
            nodeCount++;

            saveEdge(sourceTableId, taskId, "CONSUMES", st.getId(), st.getName(), null, "sync", "任务消费表", tenantId);
            saveEdge(taskId, targetTableId, "PRODUCES", st.getId(), st.getName(), null, "sync", "任务产出表", tenantId);
            saveEdge(sourceTableId, targetTableId, "FLOWS_TO", st.getId(), st.getName(), null, "sync", "表级数据流向", tenantId);
            edgeCount += 3;
        }

        // 3. Ingestion tasks: TASK + PRODUCES
        for (IngestionTaskEntity it : ingestionTaskRepository.findAll()) {
            String targetTable = normalizeTableName(it.getTargetTable());
            String targetTableId = ensureTableNode(targetTable, null, tenantId);
            String taskId = taskNodeId("INGESTION", it.getName(), tenantId);
            saveTaskNode(taskId, "INGESTION", it.getName(), tenantId);
            saveEdge(taskId, targetTableId, "PRODUCES", it.getId(), it.getName(), null, "ingestion", "采集产出表", tenantId);
            nodeCount += 2;
            edgeCount++;
        }

        // 4. Quality rules: TASK + CONSUMES
        for (QualityRuleEntity qr : qualityRuleRepository.findAll()) {
            String tableName = normalizeTableName(qr.getTableName());
            String tableId = ensureTableNode(tableName, null, tenantId);
            String taskId = taskNodeId("QUALITY", qr.getName(), tenantId);
            saveTaskNode(taskId, "QUALITY", qr.getName(), tenantId);
            saveEdge(tableId, taskId, "CONSUMES", qr.getId(), qr.getName(), null, "quality", "质量规则消费表", tenantId);
            nodeCount += 2;
            edgeCount++;
        }

        // 5. Workflow DAG: TASK nodes + SQL parsed lineage (FLOWS_TO / DERIVED_FROM)
        for (WorkflowEdgeEntity we : workflowEdgeRepository.findAll()) {
            Optional<WorkflowNodeEntity> srcOpt = workflowNodeRepository.findById(we.getSourceNodeId());
            Optional<WorkflowNodeEntity> tgtOpt = workflowNodeRepository.findById(we.getTargetNodeId());
            if (srcOpt.isEmpty() || tgtOpt.isEmpty()) continue;
            WorkflowNodeEntity src = srcOpt.get();
            WorkflowNodeEntity tgt = tgtOpt.get();
            String taskId = taskNodeId("WORKFLOW", src.getName() + "->" + tgt.getName(), tenantId);
            saveTaskNode(taskId, "WORKFLOW", src.getName() + " -> " + tgt.getName(), tenantId);
            nodeCount++;

            int[] added = collectWorkflowLineage(src, tgt, taskId, tenantId);
            edgeCount += added[0];
            nodeCount += added[1];
        }

        // 5.5 Resource files: FILE 节点 + LOAD/EXPORT 边(资源中心 × 血缘)
        // 关联信息持久化在资源表(table_id + source_type), 不依赖已删旧边
        for (com.temporedata.guard.modules.common.resource.entity.ResourceEntity r :
                resourceRepository.findAll()) {
            if (r.getLineageable() != null && !r.getLineageable()) continue;
            String nodeId = taskNodeId("FILE", r.getName(), tenantId);
            saveTaskNode(nodeId, "FILE", r.getName(), tenantId);
            nodeCount++;
            String tableId = r.getTableId();
            if (tableId == null || tableId.isBlank()) continue;
            String tblNodeId = ensureTableNode(normalizeTableName(tableId), null, tenantId);
            if ("EXPORT".equalsIgnoreCase(r.getSourceType())) {
                saveEdge(tblNodeId, nodeId, "EXPORT", r.getId(), r.getName(), null, "resource", "导出文件", tenantId);
            } else {
                saveEdge(nodeId, tblNodeId, "LOAD", r.getId(), r.getName(), null, "resource", "文件加载入湖", tenantId);
            }
            edgeCount++;
        }

        // 6. Manual / legacy relations (temporedata_guard_meta_lineage): MANUAL -> FLOWS_TO
        for (MetaLineageEntity ml : metaLineageRepository.findByTenantId(tenantId)) {
            String srcName = ml.getSourceTableName();
            String tgtName = ml.getTargetTableName();
            if (srcName == null || tgtName == null || srcName.isBlank() || tgtName.isBlank()
                    || "Unknown".equalsIgnoreCase(srcName) || "Unknown".equalsIgnoreCase(tgtName)) {
                continue;
            }
            String srcId = ensureTableNode(srcName, null, tenantId);
            String tgtId = ensureTableNode(tgtName, null, tenantId);
            nodeCount += 2;
            saveEdge(srcId, tgtId, "FLOWS_TO", null, ml.getRelationName(), ml.getTransformationSql(),
                    ml.getRelationType(), ml.getDescription(), tenantId);
            edgeCount++;
        }

        // 7. Legacy field-level lineage (temporedata_guard_meta_field_lineage): DERIVED_FROM edges
        Map<String, String> tableIdToNodeId = new HashMap<>();
        for (TableMetadataEntity t : tables) {
            tableIdToNodeId.put(t.getId(), tableNodeId(t.getTableName()));
        }
        for (TableMetadataEntity t : tables) {
            String tableId = tableNodeId(t.getTableName());
            for (MetaFieldLineageEntity fl : metaFieldLineageRepository.findBySourceTableId(t.getId())) {
                String tgtTableNodeId = tableIdToNodeId.get(fl.getTargetTableId());
                if (tgtTableNodeId == null) tgtTableNodeId = fl.getTargetTableId();
                String srcColId = columnId(tableId, fl.getSourceColumnName());
                String tgtColId = columnId(tgtTableNodeId, fl.getTargetColumnName());
                saveColumnNode(tableId, t.getTableName(), fl.getSourceColumnName(), null, tenantId);
                saveColumnNode(tgtTableNodeId, tgtTableNodeId, fl.getTargetColumnName(), null, tenantId);
                nodeCount += 2;
                saveEdge(srcColId, tgtColId, "DERIVED_FROM", null, fl.getComment(),
                        null, fl.getTransformationLogic(), "字段级血缘", tenantId);
                edgeCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalNodes", nodeCount);
        result.put("totalEdges", edgeCount);
        result.put("tenantId", tenantId);
        LineageWsHub.publish(tenantId, "lineage.updated", "rebuild", null);
        return result;
    }

    // ==================== Incremental Collection ====================

    /**
     * Event-driven collection: called after a task executes successfully.
     * Deletes the task's old edges and rebuilds them from current state (idempotent).
     * Then invalidates cache and pushes a WebSocket event to live clients.
     */
    @Transactional
    /** 数据接口血缘: 接口 SQL 解析来源表 -> API 节点 + CONSUMES 边(跨层血缘: 表→接口出口) */
    public void collectApiLineage(String tenantId, String apiId, String apiName, String sql) {
        if (tenantId == null || apiId == null) return;
        edgeRepository.deleteByTenantIdAndTaskId(tenantId, apiId);
        if (sql == null || sql.isBlank()) return;
        try {
            com.temporedata.guard.api.meta.SqlLineageRes res = sqlLineageParser.parse(sql);
            String taskId = taskNodeId("API", apiName, tenantId);
            saveTaskNode(taskId, "API", apiName, tenantId);
            if (res != null && res.getInputTables() != null) {
                for (String t : res.getInputTables()) {
                    String tableId = ensureTableNode(normalizeTableName(t), null, tenantId);
                    saveEdge(tableId, taskId, "CONSUMES", apiId, apiName, sql, "api", "接口消费表", tenantId);
                }
            }
        } catch (Exception e) {
            log.warn("collectApiLineage failed: {}", e.getMessage());
        }
    }

    /**
     * 资源血缘登记(资源中心 × 血缘):
     * FILE 节点 + LOAD(文件→表) / EXPORT(表→文件) 边, 幂等(id 唯一)。
     */
    @Transactional
    public void collectResourceLineage(String tenantId, String resourceId, String resourceName,
                                       String tableName, String relationType) {
        if (tenantId == null || resourceId == null) return;
        String nodeId = taskNodeId("FILE", resourceName == null ? resourceId : resourceName, tenantId);
        saveTaskNode(nodeId, "FILE", resourceName == null ? resourceId : resourceName, tenantId);
        // 幂等: 删除该资源同类型旧边后重建(重复登记不产生重复边)
        String edgeType = "EXPORT".equalsIgnoreCase(relationType) ? "EXPORT" : "LOAD";
        edgeRepository.deleteByTenantIdAndTaskIdAndEdgeType(tenantId, resourceId, edgeType);
        if (tableName == null || tableName.isBlank()) return;
        String tableId = ensureTableNode(normalizeTableName(tableName), null, tenantId);
        if ("EXPORT".equalsIgnoreCase(relationType)) {
            // 表 → 文件(湖仓/报表导出)
            saveEdge(tableId, nodeId, "EXPORT", resourceId, resourceName, null, "resource", "导出文件", tenantId);
        } else {
            // 文件 → 表(数据文件入湖)
            saveEdge(nodeId, tableId, "LOAD", resourceId, resourceName, null, "resource", "文件加载入湖", tenantId);
        }
    }

    /** 资源血缘查询: 返回该资源节点的上下游链路 */
    public java.util.List<java.util.Map<String, Object>> resourceLineage(String tenantId, String nodeId) {
        java.util.List<java.util.Map<String, Object>> out = new ArrayList<>();
        java.util.List<LineageEdgeEntity> edges = edgeRepository.findByTenantId(tenantId);
        for (LineageEdgeEntity e : edges) {
            if (nodeId.equals(e.getSourceId()) || nodeId.equals(e.getTargetId())) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("edgeId", e.getId());
                m.put("edgeType", e.getEdgeType());
                m.put("sourceId", e.getSourceId());
                m.put("targetId", e.getTargetId());
                m.put("taskName", e.getTaskName());
                m.put("description", e.getDescription());
                out.add(m);
            }
        }
        return out;
    }

    public void collectTaskLineage(String tenantId, String sourceType, String taskId) {
        if (tenantId == null || sourceType == null || taskId == null) return;
        switch (sourceType) {
            case "SYNC": {
                edgeRepository.deleteByTenantIdAndTaskId(tenantId, taskId);
                Optional<SyncTaskEntity> opt = syncTaskRepository.findById(taskId);
                if (opt.isPresent()) {
                    collectSyncEdge(tenantId, opt.get());
                }
                break;
            }
            case "INGESTION": {
                edgeRepository.deleteByTenantIdAndTaskId(tenantId, taskId);
                Optional<IngestionTaskEntity> opt = ingestionTaskRepository.findById(taskId);
                if (opt.isPresent()) {
                    collectIngestionEdge(tenantId, opt.get());
                }
                break;
            }
            case "QUALITY": {
                edgeRepository.deleteByTenantIdAndTaskId(tenantId, taskId);
                Optional<QualityRuleEntity> opt = qualityRuleRepository.findById(taskId);
                if (opt.isPresent()) {
                    collectQualityEdge(tenantId, opt.get());
                }
                break;
            }
            case "WORKFLOW": {
                edgeRepository.deleteByTenantIdAndTaskIdLike(tenantId, "%:WORKFLOW:%");
                for (WorkflowEdgeEntity we : workflowEdgeRepository.findByWorkflowId(taskId)) {
                    Optional<WorkflowNodeEntity> srcOpt = workflowNodeRepository.findById(we.getSourceNodeId());
                    Optional<WorkflowNodeEntity> tgtOpt = workflowNodeRepository.findById(we.getTargetNodeId());
                    if (srcOpt.isEmpty() || tgtOpt.isEmpty()) continue;
                    collectWorkflowLineage(srcOpt.get(), tgtOpt.get(),
                            taskNodeId("WORKFLOW", srcOpt.get().getName() + "->" + tgtOpt.get().getName(), tenantId),
                            tenantId);
                }
                break;
            }
            default:
                return;
        }
        cacheService.clear(tenantId);
        LineageWsHub.publish(tenantId, "lineage.updated", sourceType, taskId);
    }

    private void collectSyncEdge(String tenantId, SyncTaskEntity st) {
        String sourceTableId = ensureTableNode(normalizeTableName(st.getSourceTable()), null, tenantId);
        String targetTableId = ensureTableNode(normalizeTableName(st.getTargetTable()), null, tenantId);
        String taskId = taskNodeId("SYNC", st.getName(), tenantId);
        saveTaskNode(taskId, "SYNC", st.getName(), tenantId);
        saveEdge(sourceTableId, taskId, "CONSUMES", st.getId(), st.getName(), null, "sync", "任务消费表", tenantId);
        saveEdge(taskId, targetTableId, "PRODUCES", st.getId(), st.getName(), null, "sync", "任务产出表", tenantId);
        saveEdge(sourceTableId, targetTableId, "FLOWS_TO", st.getId(), st.getName(), null, "sync", "表级数据流向", tenantId);
    }

    private void collectIngestionEdge(String tenantId, IngestionTaskEntity it) {
        String targetTableId = ensureTableNode(normalizeTableName(it.getTargetTable()), null, tenantId);
        String taskId = taskNodeId("INGESTION", it.getName(), tenantId);
        saveTaskNode(taskId, "INGESTION", it.getName(), tenantId);
        saveEdge(taskId, targetTableId, "PRODUCES", it.getId(), it.getName(), null, "ingestion", "采集产出表", tenantId);
    }

    private void collectQualityEdge(String tenantId, QualityRuleEntity qr) {
        String tableId = ensureTableNode(normalizeTableName(qr.getTableName()), null, tenantId);
        String taskId = taskNodeId("QUALITY", qr.getName(), tenantId);
        saveTaskNode(taskId, "QUALITY", qr.getName(), tenantId);
        saveEdge(tableId, taskId, "CONSUMES", qr.getId(), qr.getName(), null, "quality", "质量规则消费表", tenantId);
    }

    private int[] collectWorkflowLineage(WorkflowNodeEntity src, WorkflowNodeEntity tgt,
                                         String taskId, String tenantId) {
        int edges = 0;
        int nodes = 0;
        String srcSql = src.getSql();
        String tgtSql = tgt.getSql();

        SqlLineageRes srcRes = sqlLineageParser.parse(srcSql);
        SqlLineageRes tgtRes = sqlLineageParser.parse(tgtSql);

        if (tgtRes != null && tgtRes.getOutputTable() != null) {
            // target node produces a table
            String outTable = tgtRes.getOutputTable();
            String outTableId = ensureTableNode(outTable, null, tenantId);
            nodes++;
            saveEdge(taskId, outTableId, "PRODUCES", taskId, src.getName() + " -> " + tgt.getName(),
                    tgtSql, "workflow", "工作流产出表", tenantId);
            edges++;

            // source node's output (or its inputs) feed the target
            String srcTable = null;
            if (srcRes != null) {
                if (srcRes.getOutputTable() != null) {
                    srcTable = srcRes.getOutputTable();
                } else if (!srcRes.getInputTables().isEmpty()) {
                    srcTable = srcRes.getInputTables().get(0);
                }
            }
            if (srcTable != null) {
                String srcTableId = ensureTableNode(srcTable, null, tenantId);
                nodes++;
                saveEdge(srcTableId, outTableId, "FLOWS_TO", taskId, src.getName() + " -> " + tgt.getName(),
                        tgtSql, "workflow", "工作流表级血缘", tenantId);
                edges++;
                saveEdge(srcTableId, taskId, "CONSUMES", taskId, src.getName() + " -> " + tgt.getName(),
                        null, "workflow", "任务消费表", tenantId);
                edges++;

                // field-level lineage DERIVED_FROM
                for (SqlLineageRes.FieldMapping fm : tgtRes.getFieldMappings()) {
                    if (fm.getSourceTable() == null || !fm.getSourceTable().equalsIgnoreCase(srcTable)) continue;
                    String srcColId = columnId(srcTableId, fm.getSourceColumn());
                    String tgtColId = columnId(outTableId, fm.getTargetColumn());
                    saveColumnNode(srcTableId, srcTable, fm.getSourceColumn(), null, tenantId);
                    saveColumnNode(outTableId, outTable, fm.getTargetColumn(), null, tenantId);
                    nodes += 2;
                    saveEdge(srcColId, tgtColId, "DERIVED_FROM", taskId, src.getName() + " -> " + tgt.getName(),
                            tgtSql, fm.getTransformation(), "字段级血缘", tenantId);
                    edges++;
                }
            }
        } else {
            // Fallback: JSON config table extraction (legacy workflow nodes)
            String srcTable = extractJsonTable(srcSql);
            String tgtTable = extractJsonTable(tgtSql);
            if (srcTable != null && tgtTable != null) {
                String srcTableId = ensureTableNode(srcTable, null, tenantId);
                String tgtTableId = ensureTableNode(tgtTable, null, tenantId);
                nodes += 2;
                saveEdge(srcTableId, tgtTableId, "FLOWS_TO", taskId, src.getName() + " -> " + tgt.getName(),
                        tgtSql, "workflow", "工作流表级血缘", tenantId);
                edges++;
            }
        }
        return new int[]{edges, nodes};
    }

    private String extractJsonTable(String config) {
        if (config == null) return null;
        if (config.contains("\"tableName\":\"")) {
            int start = config.indexOf("\"tableName\":\"") + 13;
            int end = config.indexOf("\"", start);
            if (end > start) return config.substring(start, end).toUpperCase();
        }
        if (config.contains("\"table\":\"")) {
            int start = config.indexOf("\"table\":\"") + 9;
            int end = config.indexOf("\"", start);
            if (end > start) return config.substring(start, end).toUpperCase();
        }
        return null;
    }

    // ==================== Graph Exploration ====================

    /**
     * Full-lineage overview: all FLOWS_TO edges laid out by topological layer.
     * Page-level default view so the graph is visible without picking a root node.
     */
    public LineageGraphRes overview(String tenantId) {
        return cacheService.get(tenantId, "overview", "-", LineageGraphRes.class, () -> doOverview(tenantId));
    }

    public LineageGraphRes doOverview(String tenantId) {
        List<LineageEdgeEntity> edges = edgeRepository.findByTenantId(tenantId).stream()
                .filter(e -> "FLOWS_TO".equals(e.getEdgeType()))
                .collect(Collectors.toList());

        Map<String, LineageNodeEntity> nodeById = new HashMap<>();
        Set<String> involved = new HashSet<>();
        for (LineageEdgeEntity e : edges) {
            involved.add(e.getSourceId());
            involved.add(e.getTargetId());
        }
        for (LineageNodeEntity n : nodeRepository.findByTenantId(tenantId)) {
            if (involved.contains(n.getId())) nodeById.put(n.getId(), n);
        }

        // Topological layering (Kahn): layer(n) = max(incoming layers) + 1
        Map<String, Integer> layerMap = new HashMap<>();
        Map<String, List<String>> outAdj = new HashMap<>();
        Map<String, List<String>> inAdj = new HashMap<>();
        for (LineageEdgeEntity e : edges) {
            outAdj.computeIfAbsent(e.getSourceId(), k -> new ArrayList<>()).add(e.getTargetId());
            inAdj.computeIfAbsent(e.getTargetId(), k -> new ArrayList<>()).add(e.getSourceId());
        }
        Queue<String> queue = new LinkedList<>();
        for (String id : nodeById.keySet()) {
            if (!inAdj.containsKey(id) || inAdj.get(id).isEmpty()) {
                layerMap.put(id, 0);
                queue.add(id);
            }
        }
        while (!queue.isEmpty()) {
            String cur = queue.poll();
            for (String next : outAdj.getOrDefault(cur, List.of())) {
                int nextLayer = Math.max(layerMap.getOrDefault(next, 0), layerMap.getOrDefault(cur, 0) + 1);
                layerMap.put(next, nextLayer);
                queue.add(next);
            }
        }
        // leftovers (cycle residuals) go to the bottom
        for (String id : nodeById.keySet()) {
            layerMap.putIfAbsent(id, 0);
        }
        int maxLayer = layerMap.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        LineageGraphRes res = new LineageGraphRes();
        res.setRootTableId("");
        res.setRootTableName("全量血缘总览");
        res.setMaxDepth(maxLayer);

        Map<String, LineageGraphRes.GraphNode> nodeMap = new LinkedHashMap<>();
        for (LineageEdgeEntity e : edges) {
            for (String id : List.of(e.getSourceId(), e.getTargetId())) {
                LineageNodeEntity n = nodeById.get(id);
                if (n == null) continue;
                if (nodeMap.containsKey(id)) continue;
                LineageGraphRes.GraphNode node = new LineageGraphRes.GraphNode();
                node.setId(n.getId());
                node.setName(n.getName());
                node.setNodeType(n.getNodeType());
                node.setCategory(layerMap.getOrDefault(id, 0) == 0 ? "root" : "downstream");
                node.setDepth(layerMap.getOrDefault(id, 0));
                node.setLayer(layerMap.getOrDefault(id, 0));
                node.setSymbolSize("TABLE".equals(n.getNodeType()) ? "30" : "24");
                node.setDatasourceName(n.getDb());
                nodeMap.put(id, node);
            }
        }

        Set<String> visitedEdges = new HashSet<>();
        List<LineageGraphRes.GraphLink> links = new ArrayList<>();
        for (LineageEdgeEntity e : edges) {
            String key = e.getSourceId() + "|" + e.getTargetId() + "|" + e.getEdgeType() + "|" + (e.getTaskId() == null ? "" : e.getTaskId());
            if (visitedEdges.contains(key)) continue;
            visitedEdges.add(key);
            LineageGraphRes.GraphLink link = new LineageGraphRes.GraphLink();
            link.setSource(e.getSourceId());
            link.setTarget(e.getTargetId());
            link.setRelationType(e.getEdgeType());
            link.setRelationName(e.getTaskName());
            link.setTransformationSql(e.getSql());
            link.setDescription(e.getDescription());
            links.add(link);
        }
        res.setNodes(new ArrayList<>(nodeMap.values()));
        res.setLinks(links);
        res.setTotalUpstream(0);
        res.setTotalDownstream(0);
        return res;
    }

    /**
     * Get lineage graph around a node. direction: both / up / down.
     * level: table (structural edges) / column (field-level edges).
     */
    public LineageGraphRes getGraph(String nodeId, String direction, int depth, String level,
                                    List<String> nodeTypes, String tenantId) {
        return cacheService.get(tenantId, "graph", nodeId + "|" + direction + "|" + depth + "|" + level + "|" + (nodeTypes == null ? "" : String.join(",", nodeTypes)),
                LineageGraphRes.class, () -> doGetGraph(nodeId, direction, depth, level, nodeTypes, tenantId));
    }

    public LineageGraphRes doGetGraph(String nodeId, String direction, int depth, String level,
                                      List<String> nodeTypes, String tenantId) {
        LineageNodeEntity root = nodeRepository.findById(nodeId)
                .filter(n -> tenantId.equals(n.getTenantId()) || "TABLE".equals(n.getNodeType()))
                .orElseThrow(() -> new BusinessException("节点不存在或无权访问"));

        LineageGraphRes res = new LineageGraphRes();
        res.setRootTableId(nodeId);
        res.setRootTableName(root.getName());

        boolean columnLevel = "column".equalsIgnoreCase(level);

        Map<String, LineageGraphRes.GraphNode> nodeMap = new LinkedHashMap<>();
        List<LineageGraphRes.GraphLink> links = new ArrayList<>();
        Set<String> visitedEdges = new HashSet<>();

        addNode(nodeMap, root, "root", 0, 0);

        // For TABLE root at column level: explode to columns + field-level neighbors
        if (columnLevel && "TABLE".equals(root.getNodeType())) {
            Set<String> colIds = new HashSet<>();
            for (LineageEdgeEntity e : edgeRepository.findByTenantIdAndSourceId(tenantId, nodeId)) {
                if (!"INCLUDES".equals(e.getEdgeType())) continue;
                LineageNodeEntity colNode = nodeRepository.findById(e.getTargetId()).orElse(null);
                if (colNode == null) continue;
                colIds.add(e.getTargetId());
                addNode(nodeMap, colNode, "column", 0, 1);
                addLink(links, visitedEdges, e, nodeMap);
            }
            // field-level edges (DERIVED_FROM) touching these columns -> add neighbor columns + edges
            for (LineageEdgeEntity e : edgeRepository.findByTenantId(tenantId)) {
                if (!"DERIVED_FROM".equals(e.getEdgeType())) continue;
                if (colIds.contains(e.getSourceId())) {
                    LineageNodeEntity tgt = nodeRepository.findById(e.getTargetId()).orElse(null);
                    if (tgt != null) {
                        colIds.add(e.getTargetId());
                        addNode(nodeMap, tgt, "downstream", 1, 1);
                        addLink(links, visitedEdges, e, nodeMap);
                    }
                } else if (colIds.contains(e.getTargetId())) {
                    LineageNodeEntity src = nodeRepository.findById(e.getSourceId()).orElse(null);
                    if (src != null) {
                        colIds.add(e.getSourceId());
                        addNode(nodeMap, src, "upstream", 1, -1);
                        addLink(links, visitedEdges, e, nodeMap);
                    }
                }
            }
            res.setNodes(new ArrayList<>(nodeMap.values()));
            res.setLinks(links);
            res.setMaxDepth(1);
            res.setTotalUpstream(0);
            res.setTotalDownstream(0);
            return res;
        }

        Set<String> visited = new HashSet<>();

        boolean goUp = direction == null || "both".equals(direction) || "up".equals(direction);
        boolean goDown = direction == null || "both".equals(direction) || "down".equals(direction);

        int[] upCount = {0};
        int[] downCount = {0};
        if (goUp) traverse(nodeId, true, depth, 0, columnLevel, nodeTypes, visited, visitedEdges, nodeMap, links, upCount, tenantId);
        if (goDown) traverse(nodeId, false, depth, 0, columnLevel, nodeTypes, visited, visitedEdges, nodeMap, links, downCount, tenantId);

        int maxFound = 0;
        for (LineageGraphRes.GraphNode n : nodeMap.values()) {
            if (n.getDepth() != null && n.getDepth() > maxFound) maxFound = n.getDepth();
            if ("root".equals(n.getCategory())) n.setLayer(0);
            else if ("upstream".equals(n.getCategory())) n.setLayer(-n.getDepth());
            else n.setLayer(n.getDepth());
        }
        res.setNodes(new ArrayList<>(nodeMap.values()));
        res.setLinks(links);
        res.setTotalUpstream(upCount[0]);
        res.setTotalDownstream(downCount[0]);
        res.setMaxDepth(maxFound);
        return res;
    }

    private void traverse(String nodeId, boolean upstream, int depth, int hop,
                          boolean columnLevel, List<String> nodeTypes,
                          Set<String> visited, Set<String> visitedEdges,
                          Map<String, LineageGraphRes.GraphNode> nodeMap,
                          List<LineageGraphRes.GraphLink> links, int[] count, String tenantId) {
        if (depth <= 0 || visited.contains(nodeId)) return;
        visited.add(nodeId);

        List<LineageEdgeEntity> edges = upstream
                ? edgeRepository.findByTenantIdAndTargetId(tenantId, nodeId)
                : edgeRepository.findByTenantIdAndSourceId(tenantId, nodeId);

        for (LineageEdgeEntity e : edges) {
            if (columnLevel) {
                if (!"DERIVED_FROM".equals(e.getEdgeType()) && !"INCLUDES".equals(e.getEdgeType())) continue;
            } else {
                // 表间流向 + 资源血缘(文件 LOAD/EXPORT)
                if (!"FLOWS_TO".equals(e.getEdgeType())
                        && !"LOAD".equals(e.getEdgeType())
                        && !"EXPORT".equals(e.getEdgeType())) {
                    continue;
                }
            }
            count[0]++;

            String otherId = upstream ? e.getSourceId() : e.getTargetId();
            LineageNodeEntity other = nodeRepository.findById(otherId).orElse(null);
            if (other == null) continue;
            if (nodeTypes != null && !nodeTypes.contains(other.getNodeType())) continue;

            String category = upstream ? "upstream" : "downstream";
            int layerSign = upstream ? -1 : 1;
            int childHop = hop + 1;
            addNode(nodeMap, other, category, childHop, layerSign * childHop);
            addLink(links, visitedEdges, e, nodeMap);

            if (!visited.contains(otherId)) {
                traverse(otherId, upstream, depth - 1, childHop, columnLevel, nodeTypes, visited, visitedEdges, nodeMap, links, count, tenantId);
            }
        }
        visited.remove(nodeId);
    }

    private void addNode(Map<String, LineageGraphRes.GraphNode> nodeMap, LineageNodeEntity n,
                         String category, int depth, int layer) {
        if (nodeMap.containsKey(n.getId())) return;
        LineageGraphRes.GraphNode node = new LineageGraphRes.GraphNode();
        node.setId(n.getId());
        node.setName(n.getName());
        node.setNodeType(n.getNodeType());
        node.setCategory(category);
        node.setDepth(depth);
        node.setLayer(layer);
        node.setSymbolSize("TABLE".equals(n.getNodeType()) ? "34" : "COLUMN".equals(n.getNodeType()) ? "22" : "28");
        node.setDatasourceName(n.getDb());
        nodeMap.put(n.getId(), node);
    }

    private void addLink(List<LineageGraphRes.GraphLink> links, Set<String> visitedEdges,
                         LineageEdgeEntity e, Map<String, LineageGraphRes.GraphNode> nodeMap) {
        String key = e.getSourceId() + "|" + e.getTargetId() + "|" + e.getEdgeType() + "|" + (e.getTaskId() == null ? "" : e.getTaskId());
        if (visitedEdges.contains(key)) return;
        visitedEdges.add(key);

        LineageGraphRes.GraphLink link = new LineageGraphRes.GraphLink();
        link.setSource(e.getSourceId());
        link.setTarget(e.getTargetId());
        link.setRelationType(e.getEdgeType());
        link.setRelationName(e.getTaskName());
        link.setTransformationSql(e.getSql());
        link.setDescription(e.getDescription());
        if ("DERIVED_FROM".equals(e.getEdgeType()) && e.getTransform() != null) {
            LineageGraphRes.FieldLink fl = new LineageGraphRes.FieldLink();
            fl.setSourceColumn(nodeMap.get(e.getSourceId()) != null ? nodeMap.get(e.getSourceId()).getName() : e.getSourceId());
            fl.setTargetColumn(nodeMap.get(e.getTargetId()) != null ? nodeMap.get(e.getTargetId()).getName() : e.getTargetId());
            fl.setTransformationLogic(e.getTransform());
            link.setFieldLinks(List.of(fl));
        }
        links.add(link);
    }

    // ==================== Impact & Lineage Tracing ====================

    public ImpactAnalysisRes impact(String nodeId, int depth, String tenantId) {
        return cacheService.get(tenantId, "impact", nodeId + "|" + depth,
                ImpactAnalysisRes.class, () -> doImpact(nodeId, depth, tenantId));
    }

    public ImpactAnalysisRes doImpact(String nodeId, int depth, String tenantId) {
        LineageNodeEntity root = nodeRepository.findById(nodeId)
                .filter(n -> tenantId.equals(n.getTenantId()) || "TABLE".equals(n.getNodeType()))
                .orElseThrow(() -> new BusinessException("节点不存在或无权访问"));

        ImpactAnalysisRes res = new ImpactAnalysisRes();
        res.setTableId(nodeId);
        res.setTableName(root.getName());

        List<ImpactAnalysisRes.ImpactPath> paths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(nodeId);
        collectPaths(nodeId, paths, new ArrayList<>(), visited, 1, depth, root.getName(), tenantId);
        paths.sort(Comparator.comparingInt(ImpactAnalysisRes.ImpactPath::getDepth));
        res.setPaths(paths);
        res.setTotalImpacted(paths.size());
        res.setMaxDepth(paths.stream().mapToInt(ImpactAnalysisRes.ImpactPath::getDepth).max().orElse(0));
        return res;
    }

    private void collectPaths(String nodeId, List<ImpactAnalysisRes.ImpactPath> paths, List<String> chain,
                              Set<String> visited, int depth, int maxDepth, String currentName, String tenantId) {
        if (depth > maxDepth) return;
        List<String> newChain = new ArrayList<>(chain);
        newChain.add(currentName);

        List<LineageEdgeEntity> edges = edgeRepository.findByTenantIdAndSourceId(tenantId, nodeId);
        for (LineageEdgeEntity e : edges) {
            if ("INCLUDES".equals(e.getEdgeType()) || "CONSUMES".equals(e.getEdgeType())) continue;
            if (visited.contains(e.getTargetId())) continue;
            visited.add(e.getTargetId());

            LineageNodeEntity tgt = nodeRepository.findById(e.getTargetId()).orElse(null);
            if (tgt == null) continue;

            ImpactAnalysisRes.ImpactPath path = new ImpactAnalysisRes.ImpactPath();
            path.setTableId(e.getTargetId());
            path.setTableName(tgt.getName());
            path.setDepth(depth);
            path.setRelationType(e.getEdgeType());
            path.setRelationName(e.getTaskName());
            List<String> fullChain = new ArrayList<>(newChain);
            fullChain.add(tgt.getName());
            path.setPathChain(fullChain);
            paths.add(path);

            collectPaths(e.getTargetId(), paths, newChain, visited, depth + 1, maxDepth, tgt.getName(), tenantId);
        }
    }

    // ==================== Field Lineage ====================

    /**
     * Field-level lineage around a single column: DERIVED_FROM edges
     * (upstream/downstream) plus the owning table's INCLUDES edge.
     */
    public LineageGraphRes fieldLineage(String tableId, String columnName, String direction, int depth, String tenantId) {
        String columnNodeId = columnId(tableId, columnName);
        return getGraph(columnNodeId, direction, depth, "column", null, tenantId);
    }

    // ==================== Path Analysis ====================

    public LineagePathRes findPath(String sourceId, String targetId, String tenantId) {
        LineagePathRes res = new LineagePathRes();
        res.setSourceId(sourceId);
        res.setTargetId(targetId);
        nodeRepository.findById(sourceId).ifPresent(n -> res.setSourceName(n.getName()));
        nodeRepository.findById(targetId).ifPresent(n -> res.setTargetName(n.getName()));

        if (sourceId.equals(targetId)) {
            res.setFound(true);
            res.getPath().add(sourceId);
            return res;
        }

        // BFS shortest path over FLOWS_TO edges
        Map<String, String> prev = new HashMap<>();
        Map<String, LineageEdgeEntity> prevEdge = new HashMap<>();
        Queue<String> queue = new LinkedList<>();
        Set<String> seen = new HashSet<>();
        queue.add(sourceId);
        seen.add(sourceId);

        while (!queue.isEmpty()) {
            String cur = queue.poll();
            if (cur.equals(targetId)) break;
            for (LineageEdgeEntity e : edgeRepository.findByTenantIdAndSourceId(tenantId, cur)) {
                if (!"FLOWS_TO".equals(e.getEdgeType())) continue;
                if (seen.contains(e.getTargetId())) continue;
                seen.add(e.getTargetId());
                prev.put(e.getTargetId(), cur);
                prevEdge.put(e.getTargetId(), e);
                queue.add(e.getTargetId());
            }
        }

        if (!seen.contains(targetId)) {
            res.setFound(false);
            return res;
        }

        res.setFound(true);
        List<String> path = new ArrayList<>();
        String cur = targetId;
        while (cur != null) {
            path.add(0, cur);
            cur = prev.get(cur);
        }
        res.setPath(path);

        List<LineagePathRes.PathHop> hops = new ArrayList<>();
        for (int i = 0; i < path.size() - 1; i++) {
            LineageEdgeEntity e = prevEdge.get(path.get(i + 1));
            if (e == null) continue;
            LineagePathRes.PathHop hop = new LineagePathRes.PathHop();
            hop.setSource(path.get(i));
            hop.setTarget(path.get(i + 1));
            hop.setEdgeType(e.getEdgeType());
            hop.setTaskName(e.getTaskName());
            hops.add(hop);
        }
        res.setHops(hops);
        return res;
    }

    // ==================== Cycle Detection ====================

    public CycleAnalysisRes findCycles(String tenantId) {
        CycleAnalysisRes res = new CycleAnalysisRes();
        List<LineageEdgeEntity> allEdges = edgeRepository.findByTenantId(tenantId).stream()
                .filter(e -> "FLOWS_TO".equals(e.getEdgeType()) || "DERIVED_FROM".equals(e.getEdgeType()))
                .collect(Collectors.toList());

        Map<String, List<String>> adj = new HashMap<>();
        for (LineageEdgeEntity e : allEdges) {
            adj.computeIfAbsent(e.getSourceId(), k -> new ArrayList<>()).add(e.getTargetId());
        }

        Set<String> white = new LinkedHashSet<>(adj.keySet());
        Set<String> gray = new LinkedHashSet<>();
        Set<String> black = new HashSet<>();
        List<List<String>> cycles = new ArrayList<>();
        Map<String, String> nodeNames = new HashMap<>();
        for (LineageNodeEntity n : nodeRepository.findByTenantId(tenantId)) {
            nodeNames.put(n.getId(), n.getName());
        }

        for (String start : new ArrayList<>(white)) {
            if (white.contains(start)) {
                dfsCycles(start, adj, white, gray, black, new ArrayList<>(), cycles, nodeNames);
            }
        }

        res.setHasCycle(!cycles.isEmpty());
        res.setCycles(cycles);
        res.setSummary(cycles.isEmpty()
                ? "未检测到循环依赖，链路健康"
                : "检测到 " + cycles.size() + " 个循环依赖环");
        return res;
    }

    private void dfsCycles(String node, Map<String, List<String>> adj,
                           Set<String> white, Set<String> gray, Set<String> black,
                           List<String> stack, List<List<String>> cycles, Map<String, String> nodeNames) {
        white.remove(node);
        gray.add(node);
        stack.add(node);

        for (String next : adj.getOrDefault(node, List.of())) {
            if (gray.contains(next)) {
                int idx = stack.indexOf(next);
                if (idx >= 0) {
                    List<String> cycle = new ArrayList<>(stack.subList(idx, stack.size()));
                    cycle.add(next);
                    List<String> named = cycle.stream()
                            .map(id -> nodeNames.getOrDefault(id, id))
                            .collect(Collectors.toList());
                    if (cycles.stream().noneMatch(c -> c.size() == named.size() && c.equals(named))) {
                        cycles.add(named);
                    }
                }
                continue;
            }
            if (white.contains(next)) {
                dfsCycles(next, adj, white, gray, black, stack, cycles, nodeNames);
            }
        }
        gray.remove(node);
        black.add(node);
        stack.remove(stack.size() - 1);
    }

    // ==================== Heat & Search ====================

    public List<HotNodeRes> heat(String tenantId) {
        return doHeat(tenantId);
    }

    public List<HotNodeRes> doHeat(String tenantId) {
        Map<String, int[]> degrees = new HashMap<>();
        List<LineageNodeEntity> nodes = nodeRepository.findByTenantId(tenantId);
        for (LineageNodeEntity n : nodes) {
            degrees.put(n.getId(), new int[2]);
        }
        for (LineageEdgeEntity e : edgeRepository.findByTenantId(tenantId)) {
            if ("FLOWS_TO".equals(e.getEdgeType()) || "DERIVED_FROM".equals(e.getEdgeType())
                    || "CONSUMES".equals(e.getEdgeType()) || "PRODUCES".equals(e.getEdgeType())) {
                int[] src = degrees.get(e.getSourceId());
                int[] tgt = degrees.get(e.getTargetId());
                if (src != null) src[1]++;
                if (tgt != null) tgt[0]++;
            }
        }
        return nodes.stream().map(n -> {
            HotNodeRes h = new HotNodeRes();
            h.setId(n.getId());
            h.setName(n.getName());
            h.setNodeType(n.getNodeType());
            h.setInDegree(degrees.getOrDefault(n.getId(), new int[2])[0]);
            h.setOutDegree(degrees.getOrDefault(n.getId(), new int[2])[1]);
            h.setTotal(h.getInDegree() + h.getOutDegree());
            return h;
        }).sorted(Comparator.comparingInt(HotNodeRes::getTotal).reversed())
                .limit(20)
                .collect(Collectors.toList());
    }

    public List<Map<String, Object>> search(String keyword, String tenantId) {
        if (keyword == null || keyword.trim().isEmpty()) return List.of();
        String kw = keyword.trim().toLowerCase();
        return nodeRepository.findByTenantId(tenantId).stream()
                .filter(n -> n.getName().toLowerCase().contains(kw)
                        || (n.getDb() != null && n.getDb().toLowerCase().contains(kw))
                        || (n.getSchema() != null && n.getSchema().toLowerCase().contains(kw)))
                .map(n -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", n.getId());
                    m.put("name", n.getName());
                    m.put("nodeType", n.getNodeType());
                    m.put("db", n.getDb());
                    m.put("schema", n.getSchema());
                    long in = edgeRepository.findByTenantIdAndTargetId(tenantId, n.getId()).size();
                    long out = edgeRepository.findByTenantIdAndSourceId(tenantId, n.getId()).size();
                    m.put("inDegree", in);
                    m.put("outDegree", out);
                    return m;
                })
                .limit(50)
                .collect(Collectors.toList());
    }

    public Map<String, Object> dumpForDebug(String tenantId) {
        Set<String> nodeIds = nodeRepository.findByTenantId(tenantId).stream()
                .map(LineageNodeEntity::getId).collect(Collectors.toSet());
        List<Map<String, Object>> dangling = new ArrayList<>();
        for (LineageEdgeEntity e : edgeRepository.findByTenantId(tenantId)) {
            if (!nodeIds.contains(e.getSourceId()) || !nodeIds.contains(e.getTargetId())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.getId());
                m.put("src", e.getSourceId() + (nodeIds.contains(e.getSourceId()) ? "(ok)" : "(MISSING)"));
                m.put("tgt", e.getTargetId() + (nodeIds.contains(e.getTargetId()) ? "(ok)" : "(MISSING)"));
                m.put("type", e.getEdgeType());
                m.put("task", e.getTaskName());
                dangling.add(m);
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("nodeCount", nodeIds.size());
        out.put("edgeCount", edgeRepository.findByTenantId(tenantId).size());
        out.put("dangling", dangling);
        List<Map<String, Object>> selfLoops = new ArrayList<>();
        for (LineageEdgeEntity e : edgeRepository.findByTenantId(tenantId)) {
            if (e.getSourceId().equals(e.getTargetId())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", e.getId());
                m.put("node", e.getSourceId());
                m.put("type", e.getEdgeType());
                m.put("task", e.getTaskName());
                selfLoops.add(m);
            }
        }
        out.put("selfLoops", selfLoops);
        List<Map<String, Object>> legacyRows = new ArrayList<>();
        for (MetaLineageEntity ml : metaLineageRepository.findByTenantId(tenantId)) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("src", ml.getSourceTableName());
            m.put("tgt", ml.getTargetTableName());
            m.put("type", ml.getRelationType());
            legacyRows.add(m);
        }
        out.put("legacyRows", legacyRows);
        return out;
    }

public String exportJson(String nodeId, int depth, String tenantId) {
        LineageGraphRes g = getGraph(nodeId, "both", depth, "table", null, tenantId);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"root\":\"").append(g.getRootTableName()).append("\",\"nodes\":[");
        boolean first = true;
        for (LineageGraphRes.GraphNode n : g.getNodes()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"id\":\"").append(n.getId()).append("\",\"name\":\"").append(n.getName())
                    .append("\",\"type\":\"").append(n.getNodeType() == null ? "" : n.getNodeType())
                    .append("\",\"category\":\"").append(n.getCategory()).append("\"}");
        }
        sb.append("],\"edges\":[");
        first = true;
        for (LineageGraphRes.GraphLink l : g.getLinks()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"source\":\"").append(l.getSource()).append("\",\"target\":\"").append(l.getTarget())
                    .append("\",\"type\":\"").append(l.getRelationType()).append("\"}");
        }
        sb.append("]}");
        return sb.toString();
    }

    // ==================== Helpers ====================

    private String saveTableNode(TableMetadataEntity t, String tenantId) {
        String id = tableNodeId(t.getTableName());
        LineageNodeEntity node = nodeRepository.findById(id).orElse(new LineageNodeEntity());
        node.setId(id);
        node.setNodeType("TABLE");
        node.setName(t.getTableName());
        node.setDb(t.getSchemaName());
        node.setSchema(t.getSchemaName());
        node.setTenantId(tenantId);
        if (node.getCreateTime() == null) node.setCreateTime(LocalDateTime.now());
        Map<String, Object> props = new LinkedHashMap<>();
        props.put("datasourceId", t.getDatasourceId());
        props.put("rowCount", t.getRowCount() == null ? 0 : t.getRowCount());
        props.put("comment", t.getComment());
        node.setProperties(props.toString());
        return nodeRepository.save(node).getId();
    }

    private String ensureTableNode(String tableName, String db, String tenantId) {
        String id = tableNodeId(tableName);
        if (nodeRepository.existsById(id)) return id;
        LineageNodeEntity node = new LineageNodeEntity();
        node.setId(id);
        node.setNodeType("TABLE");
        node.setName(tableName);
        node.setDb(db);
        node.setTenantId(tenantId);
        node.setCreateTime(LocalDateTime.now());
        return nodeRepository.save(node).getId();
    }

    private void saveColumnNode(String tableId, String tableName, ColumnMetadataEntity c, String tenantId) {
        saveColumnNode(tableId, tableName, c.getColumnName(), c.getDataType(), tenantId);
    }

    private void saveColumnNode(String tableId, String tableName, String columnName, String dataType, String tenantId) {
        String id = columnId(tableId, columnName);
        if (nodeRepository.existsById(id)) return;
        LineageNodeEntity node = new LineageNodeEntity();
        node.setId(id);
        node.setNodeType("COLUMN");
        node.setName(columnName);
        node.setDb(tableName);
        node.setSchema(columnName);
        node.setTenantId(tenantId);
        node.setProperties(dataType == null ? "{}" : "{\"dataType\":\"" + dataType + "\"}");
        node.setCreateTime(LocalDateTime.now());
        nodeRepository.save(node);
    }

    private void saveTaskNode(String taskId, String type, String name, String tenantId) {
        if (nodeRepository.existsById(taskId)) return;
        LineageNodeEntity node = new LineageNodeEntity();
        node.setId(taskId);
        node.setNodeType("TASK");
        node.setName(name);
        node.setSchema(type);
        node.setTenantId(tenantId);
        node.setCreateTime(LocalDateTime.now());
        nodeRepository.save(node);
    }

    private void saveEdge(String sourceId, String targetId, String edgeType, String taskId, String taskName,
                          String sql, String transform, String description, String tenantId) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        if (edgeRepository.existsById(id)) return;
        LineageEdgeEntity e = new LineageEdgeEntity();
        e.setId(id);
        e.setSourceId(sourceId);
        e.setTargetId(targetId);
        e.setEdgeType(edgeType);
        e.setTaskId(taskId);
        e.setTaskName(taskName);
        e.setSql(sql);
        e.setTransform(transform);
        e.setDescription(description);
        e.setTenantId(tenantId);
        e.setCreateTime(LocalDateTime.now());
        e.setUpdateTime(LocalDateTime.now());
        edgeRepository.save(e);
    }

    private String tableNodeId(String tableName) {
        return "T:" + normalizeTableName(tableName);
    }

    private String columnId(String tableId, String columnName) {
        return "C:" + tableId + ":" + columnName;
    }

    private String taskNodeId(String type, String name, String tenantId) {
        return "J:" + tenantId + ":" + type + ":" + name;
    }

    private String normalizeTableName(String name) {
        if (name == null) return "UNKNOWN";
        String n = name;
        int dot = n.indexOf('.');
        if (dot >= 0) n = n.substring(dot + 1);
        return n.replace("\"", "").toUpperCase();
    }
}