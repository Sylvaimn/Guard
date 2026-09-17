package com.temporedata.guard.modules.dev.meta.service;
import com.temporedata.guard.modules.dev.catalog.entity.ColumnMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.ColumnMetadataRepository;
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

import cn.hutool.core.util.StrUtil;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.meta.*;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.dev.catalog.service.CatalogService;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.ingestion.entity.IngestionTaskEntity;
import com.temporedata.guard.modules.dev.ingestion.repository.IngestionTaskRepository;
import com.temporedata.guard.modules.dev.meta.entity.*;
import com.temporedata.guard.modules.dev.meta.repository.*;
import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;
import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Metadata collection and lineage analysis service.
 * Auto-collects table/column metadata from registered datasources,
 * builds multi-level lineage graph from sync/ingestion/workflow/quality tasks.
 *
 * Note: table/column metadata now lives in the unified catalog storage
 * (temporedata_guard_table_metadata / temporedata_guard_column_metadata); this service only reads it.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaService {

    private final TableMetadataRepository tableMetadataRepository;
    private final ColumnMetadataRepository columnMetadataRepository;
    private final MetaLineageRepository metaLineageRepository;
    private final DatasourceRepository datasourceRepository;
    private final SyncTaskRepository syncTaskRepository;
    private final IngestionTaskRepository ingestionTaskRepository;
    private final QualityRuleRepository qualityRuleRepository;
    private final WorkflowNodeRepository workflowNodeRepository;
    private final WorkflowEdgeRepository workflowEdgeRepository;
    private final MetaFieldLineageRepository metaFieldLineageRepository;
    private final SqlLineageParser sqlLineageParser;
    private final CatalogService catalogService;

    // ==================== Overview ====================

    /**
     * Datasource-level metadata overview: table/column/row counts per datasource.
     */
    public Map<String, Object> getOverview(String tenantId) {
        Map<String, Object> result = new LinkedHashMap<>();

        List<TableMetadataEntity> tables = tableMetadataRepository.findAll();
        List<ColumnMetadataEntity> columns = columnMetadataRepository.findAll();
        List<DatasourceEntity> datasources = datasourceRepository.findAll();

        long totalRows = tables.stream()
                .mapToLong(t -> t.getRowCount() != null ? t.getRowCount() : 0)
                .sum();
        result.put("totalTables", tables.size());
        result.put("totalColumns", columns.size());
        result.put("totalRows", totalRows);

        List<Map<String, Object>> dsList = new ArrayList<>();
        for (DatasourceEntity ds : datasources) {
            List<TableMetadataEntity> dsTables = tables.stream()
                    .filter(t -> ds.getId().equals(t.getDatasourceId()))
                    .collect(Collectors.toList());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("datasourceId", ds.getId());
            item.put("datasourceName", ds.getName());
            item.put("datasourceType", ds.getType());
            item.put("tableCount", dsTables.size());
            item.put("columnCount", dsTables.stream()
                    .mapToInt(t -> (int) columns.stream().filter(c -> t.getId().equals(c.getTableMetadataId())).count())
                    .sum());
            item.put("rowCount", dsTables.stream()
                    .mapToLong(t -> t.getRowCount() != null ? t.getRowCount() : 0)
                    .sum());
            item.put("lastSyncTime", dsTables.stream()
                    .map(TableMetadataEntity::getSyncTime)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .map(LocalDateTime::toString)
                    .orElse(null));
            item.put("collected", !dsTables.isEmpty());
            dsList.add(item);
        }
        result.put("datasources", dsList);
        result.put("unCollectedCount", (int) dsList.stream().filter(d -> !(Boolean) d.get("collected")).count());

        return result;
    }

    // ==================== Metadata Collection ====================

    /**
     * Collect metadata via the unified catalog service (single source of truth).
     */
    @Transactional
    public Map<String, Object> syncMetadata(String datasourceId, String tenantId) {
        return catalogService.sync(datasourceId);
    }
    /**
     * Sync metadata for all datasources.
     */
    @Transactional
    public Map<String, Object> syncAllMetadata(String tenantId) {
        List<DatasourceEntity> datasources = datasourceRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
        int totalTables = 0;
        int totalColumns = 0;
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;
        List<String> skippedReasons = new ArrayList<>();

        for (DatasourceEntity ds : datasources) {
            try {
                Map<String, Object> result = syncMetadata(ds.getId(), tenantId);
                if (Boolean.TRUE.equals(result.get("skipped"))) {
                    skippedCount++;
                    skippedReasons.add(ds.getName() + ": " + result.get("reason"));
                    continue;
                }
                totalTables += (int) result.get("tableCount");
                totalColumns += (int) result.get("columnCount");
                successCount++;
            } catch (Exception e) {
                log.error("Failed to sync metadata for datasource: {}", ds.getId(), e);
                failCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalDatasources", datasources.size());
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("skippedCount", skippedCount);
        result.put("skippedReasons", skippedReasons);
        result.put("totalTables", totalTables);
        result.put("totalColumns", totalColumns);
        return result;
    }

    // ==================== Lineage ====================

    /**
     * Build lineage relationships from all task types.
     * Sources: sync tasks, ingestion tasks, workflow DAG, quality rules.
     */
    @Transactional
    public void buildLineage(String datasourceId, String tenantId) {
        // Clear existing auto-generated lineage for tables in this datasource (keep MANUAL)
        List<TableMetadataEntity> tables = tableMetadataRepository.findByDatasourceId(datasourceId);
        for (TableMetadataEntity t : tables) {
            List<MetaLineageEntity> toDelete =
                    metaLineageRepository.findBySourceTableIdOrTargetTableId(t.getId(), t.getId());
            for (MetaLineageEntity l : toDelete) {
                if (!"MANUAL".equals(l.getRelationType())) {
                    metaLineageRepository.delete(l);
                }
            }
        }

        Set<String> savedKeys = new HashSet<>();

        // 1. Sync tasks: source -> target
        List<SyncTaskEntity> syncTasks = syncTaskRepository.findAll();
        for (SyncTaskEntity st : syncTasks) {
            TableMetadataEntity sourceTable = findTable(st.getSourceDatasourceId(), st.getSourceTable());
            TableMetadataEntity targetTable = findTable(st.getTargetDatasourceId(), st.getTargetTable());
            if (sourceTable != null && targetTable != null) {
                saveLineage(sourceTable, targetTable, "SYNC", st.getName(), savedKeys, tenantId);
            }
        }

        // 2. Ingestion tasks: FILE_UPLOAD -> target
        List<IngestionTaskEntity> ingestions = ingestionTaskRepository.findAll();
        for (IngestionTaskEntity it : ingestions) {
            TableMetadataEntity targetTable = findTable(datasourceId, it.getTargetTable());
            if (targetTable != null) {
                saveLineageWithSource(it.getType() != null ? it.getType() : "FILE",
                        it.getType() != null ? it.getType() : "FILE_UPLOAD",
                        targetTable, "INGESTION", it.getName(), savedKeys, tenantId);
            }
        }

        // 3. Workflow DAG edges: node connections represent data flow
        List<WorkflowEdgeEntity> edges = workflowEdgeRepository.findAll();
        for (WorkflowEdgeEntity edge : edges) {
            Optional<WorkflowNodeEntity> sourceNode = workflowNodeRepository.findById(edge.getSourceNodeId());
            Optional<WorkflowNodeEntity> targetNode = workflowNodeRepository.findById(edge.getTargetNodeId());
            if (sourceNode.isPresent() && targetNode.isPresent()) {
                WorkflowNodeEntity src = sourceNode.get();
                WorkflowNodeEntity tgt = targetNode.get();
                // Try to extract table references from node SQL
                extractAndSaveLineage(src.getSql(), tgt.getSql(), "WORKFLOW",
                        src.getName() + " -> " + tgt.getName(), savedKeys, tenantId);
            }
        }

        // 4. Quality rules: table -> quality check
        List<QualityRuleEntity> rules = qualityRuleRepository.findAll();
        for (QualityRuleEntity r : rules) {
            TableMetadataEntity table = findTable(datasourceId, r.getTableName());
            if (table != null) {
                saveLineageWithTarget(table, "QUALITY_CHECK", "质量检查",
                        "QUALITY", r.getName(), savedKeys, tenantId);
            }
        }
    }

    /**
     * Get multi-level lineage for a table (recursive traversal).
     */
    public MetaLineageRes getLineage(String tableId, int maxDepth) {
        TableMetadataEntity table = tableMetadataRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException("表元数据不存在"));

        MetaLineageRes res = new MetaLineageRes();
        res.setTableId(table.getId());
        res.setTableName(table.getTableName());

        Set<String> visited = new HashSet<>();
        List<MetaLineageRes.LineageNode> upstream = new ArrayList<>();
        List<MetaLineageRes.LineageNode> downstream = new ArrayList<>();
        List<MetaLineageRes.LineageEdge> edges = new ArrayList<>();

        // Recursive traversal by table name (legacy lineage relations reference
        // table names; catalog table ids may change across re-syncs)
        String tableName = table.getTableName();
        traverseUpstream(tableName, upstream, edges, visited, 1, maxDepth);
        visited.clear();
        traverseDownstream(tableName, downstream, edges, visited, 1, maxDepth);

        res.setUpstream(upstream);
        res.setDownstream(downstream);
        res.setEdges(edges);
        return res;
    }

    private String resolveTableIdByName(String tableName) {
        List<TableMetadataEntity> matches = tableMetadataRepository.findByTableName(tableName);
        return matches.isEmpty() ? null : matches.get(0).getId();
    }

    private void traverseUpstream(String tableName, List<MetaLineageRes.LineageNode> nodes,
                                   List<MetaLineageRes.LineageEdge> edges,
                                   Set<String> visited, int depth, int maxDepth) {
        if (depth > maxDepth || visited.contains(tableName)) return;
        visited.add(tableName);

        List<MetaLineageEntity> lineages = metaLineageRepository.findByTargetTableName(tableName);
        for (MetaLineageEntity l : lineages) {
            MetaLineageRes.LineageNode node = new MetaLineageRes.LineageNode();
            node.setTableId(resolveTableIdByName(l.getSourceTableName()));
            node.setTableName(l.getSourceTableName());
            node.setRelationType(l.getRelationType());
            node.setRelationName(l.getRelationName());
            node.setDepth(depth);
            nodes.add(node);

            MetaLineageRes.LineageEdge edge = new MetaLineageRes.LineageEdge();
            edge.setId(l.getId());
            edge.setSourceTableId(resolveTableIdByName(l.getSourceTableName()));
            edge.setSourceTableName(l.getSourceTableName());
            edge.setTargetTableId(resolveTableIdByName(l.getTargetTableName()));
            edge.setTargetTableName(l.getTargetTableName());
            edge.setRelationType(l.getRelationType());
            edge.setRelationName(l.getRelationName());
            edges.add(edge);

            traverseUpstream(l.getSourceTableName(), nodes, edges, visited, depth + 1, maxDepth);
        }
    }

    private void traverseDownstream(String tableName, List<MetaLineageRes.LineageNode> nodes,
                                     List<MetaLineageRes.LineageEdge> edges,
                                     Set<String> visited, int depth, int maxDepth) {
        if (depth > maxDepth || visited.contains(tableName)) return;
        visited.add(tableName);

        List<MetaLineageEntity> lineages = metaLineageRepository.findBySourceTableName(tableName);
        for (MetaLineageEntity l : lineages) {
            MetaLineageRes.LineageNode node = new MetaLineageRes.LineageNode();
            node.setTableId(resolveTableIdByName(l.getTargetTableName()));
            node.setTableName(l.getTargetTableName());
            node.setRelationType(l.getRelationType());
            node.setRelationName(l.getRelationName());
            node.setDepth(depth);
            nodes.add(node);

            MetaLineageRes.LineageEdge edge = new MetaLineageRes.LineageEdge();
            edge.setId(l.getId());
            edge.setSourceTableId(resolveTableIdByName(l.getSourceTableName()));
            edge.setSourceTableName(l.getSourceTableName());
            edge.setTargetTableId(resolveTableIdByName(l.getTargetTableName()));
            edge.setTargetTableName(l.getTargetTableName());
            edge.setRelationType(l.getRelationType());
            edge.setRelationName(l.getRelationName());
            edges.add(edge);

            traverseDownstream(l.getTargetTableName(), nodes, edges, visited, depth + 1, maxDepth);
        }
    }

    // ==================== Query ====================

    public List<MetaTableRes> listTables(String tenantId) {
        List<TableMetadataEntity> tables = tableMetadataRepository.findByTenantIdOrderBySyncTimeDesc(tenantId);
        return tables.stream().map(this::toTableRes).collect(Collectors.toList());
    }

    public List<MetaTableRes> listTablesByDatasource(String datasourceId) {
        List<TableMetadataEntity> tables = tableMetadataRepository.findByDatasourceId(datasourceId);
        return tables.stream().map(this::toTableRes).collect(Collectors.toList());
    }

    public MetaTableRes getTableDetail(String tableId) {
        TableMetadataEntity table = tableMetadataRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException("表元数据不存在"));
        MetaTableRes res = toTableRes(table);
        List<ColumnMetadataEntity> columns = columnMetadataRepository.findByTableMetadataIdOrderByOrdinalPosition(tableId);
        res.setColumns(columns.stream().map(this::toColumnRes).collect(Collectors.toList()));
        return res;
    }

    public List<MetaColumnRes> getColumns(String tableId) {
        return columnMetadataRepository.findByTableMetadataIdOrderByOrdinalPosition(tableId)
                .stream().map(this::toColumnRes).collect(Collectors.toList());
    }

    // ==================== Helper Methods ====================

    private MetaTableRes toTableRes(TableMetadataEntity entity) {
        MetaTableRes res = new MetaTableRes();
        res.setId(entity.getId());
        res.setDatasourceId(entity.getDatasourceId());
        res.setSchemaName(entity.getSchemaName());
        res.setTableName(entity.getTableName());
        res.setComment(entity.getComment());
        res.setRowCount(entity.getRowCount());
        res.setSyncTime(entity.getSyncTime());
        res.setCreateTime(entity.getSyncTime());
        // Lookup datasource name
        datasourceRepository.findById(entity.getDatasourceId())
                .ifPresent(ds -> res.setDatasourceName(ds.getName()));
        return res;
    }

    private MetaColumnRes toColumnRes(ColumnMetadataEntity entity) {
        MetaColumnRes res = new MetaColumnRes();
        res.setId(entity.getId());
        res.setTableId(entity.getTableMetadataId());
        res.setColumnName(entity.getColumnName());
        res.setColumnType(entity.getDataType());
        res.setColumnSize(entity.getColumnSize());
        res.setDecimalDigits(entity.getDecimalDigits());
        res.setNullable(entity.getIsNullable());
        res.setDefaultValue(entity.getDefaultValue());
        res.setComment(entity.getComment());
        res.setPrimaryKey(entity.getIsPrimaryKey());
        res.setOrdinalPosition(entity.getOrdinalPosition());
        return res;
    }

    private TableMetadataEntity findTable(String datasourceId, String tableName) {
        if (datasourceId == null || tableName == null) return null;
        return tableMetadataRepository.findByDatasourceIdAndSchemaNameAndTableName(
                datasourceId, "PUBLIC", tableName);
    }

    private void saveLineage(TableMetadataEntity source, TableMetadataEntity target,
                              String type, String name, Set<String> savedKeys, String tenantId) {
        String key = source.getId() + "_" + target.getId() + "_" + type;
        if (savedKeys.contains(key)) return;
        savedKeys.add(key);

        MetaLineageEntity entity = new MetaLineageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setSourceTableId(source.getId());
        entity.setSourceTableName(source.getTableName());
        entity.setTargetTableId(target.getId());
        entity.setTargetTableName(target.getTableName());
        entity.setRelationType(type);
        entity.setRelationName(name);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        metaLineageRepository.save(entity);
    }

    private void saveLineageWithSource(String sourceType, String sourceName,
                                        TableMetadataEntity target, String type, String name,
                                        Set<String> savedKeys, String tenantId) {
        String key = sourceType + "_" + target.getId() + "_" + type;
        if (savedKeys.contains(key)) return;
        savedKeys.add(key);

        MetaLineageEntity entity = new MetaLineageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setSourceTableId("EXTERNAL_" + sourceType);
        entity.setSourceTableName(sourceName);
        entity.setTargetTableId(target.getId());
        entity.setTargetTableName(target.getTableName());
        entity.setRelationType(type);
        entity.setRelationName(name);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        metaLineageRepository.save(entity);
    }

    private void saveLineageWithTarget(TableMetadataEntity source, String targetType, String targetName,
                                        String type, String name, Set<String> savedKeys, String tenantId) {
        String key = source.getId() + "_" + targetType + "_" + type;
        if (savedKeys.contains(key)) return;
        savedKeys.add(key);

        MetaLineageEntity entity = new MetaLineageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setSourceTableId(source.getId());
        entity.setSourceTableName(source.getTableName());
        entity.setTargetTableId("EXTERNAL_" + targetType);
        entity.setTargetTableName(targetName);
        entity.setRelationType(type);
        entity.setRelationName(name);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        metaLineageRepository.save(entity);
    }

    private void extractAndSaveLineage(String sourceConfig, String targetConfig,
                                        String type, String name, Set<String> savedKeys, String tenantId) {
        if (sourceConfig == null || targetConfig == null) return;

        // Real SQL parsing: source node's output table feeds target node's output table
        SqlLineageRes srcRes = sqlLineageParser.parse(sourceConfig);
        SqlLineageRes tgtRes = sqlLineageParser.parse(targetConfig);
        if (tgtRes == null || tgtRes.getOutputTable() == null) return;

        String targetTable = tgtRes.getOutputTable();
        String sourceTable = null;
        if (srcRes != null) {
            if (srcRes.getOutputTable() != null) {
                sourceTable = srcRes.getOutputTable();
            } else if (!srcRes.getInputTables().isEmpty()) {
                sourceTable = srcRes.getInputTables().get(0);
            }
        }
        if (sourceTable == null) return;

        String key = sourceTable + "_" + targetTable + "_" + type;
        if (savedKeys.contains(key)) return;
        savedKeys.add(key);

        MetaLineageEntity entity = new MetaLineageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setSourceTableId("WORKFLOW_SRC_" + sourceTable);
        entity.setSourceTableName(sourceTable);
        entity.setTargetTableId("WORKFLOW_TGT_" + targetTable);
        entity.setTargetTableName(targetTable);
        entity.setRelationType(type);
        entity.setRelationName(name);
        entity.setTransformationSql(targetConfig);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        metaLineageRepository.save(entity);

        // Field-level lineage parsed from target SQL
        for (SqlLineageRes.FieldMapping fm : tgtRes.getFieldMappings()) {
            if (fm.getSourceTable() != null && fm.getSourceTable().equalsIgnoreCase(sourceTable)) {
                MetaFieldLineageEntity fl = new MetaFieldLineageEntity();
                fl.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
                fl.setLineageId(entity.getId());
                fl.setSourceTableId(entity.getSourceTableId());
                fl.setSourceColumnName(fm.getSourceColumn());
                fl.setTargetTableId(entity.getTargetTableId());
                fl.setTargetColumnName(fm.getTargetColumn());
                fl.setTransformationLogic(fm.getTransformation());
                fl.setCreateTime(LocalDateTime.now());
                metaFieldLineageRepository.save(fl);
            }
        }
    }
}

