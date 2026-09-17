package com.temporedata.guard.modules.dev.meta.service;
import com.temporedata.guard.modules.dev.meta.repository.MetaColumnRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaFieldLineageEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaFieldLineageRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaLineageEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaLineageRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaTableEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaTableRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.meta.*;

import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.meta.entity.*;
import com.temporedata.guard.modules.dev.meta.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced lineage analysis service.
 * Provides graph construction, impact analysis, field-level lineage, and search.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetaLineageService {

    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final MetaLineageRepository metaLineageRepository;
    private final MetaFieldLineageRepository metaFieldLineageRepository;
    private final DatasourceRepository datasourceRepository;
    private final SqlLineageParser sqlLineageParser;

    // ==================== Graph Construction ====================

    /**
     * Build a full lineage graph (nodes + links) for ECharts force-directed visualization.
     * Traverses both upstream and downstream from the root table.
     */
    public LineageGraphRes buildLineageGraph(String tableId, int maxDepth) {
        MetaTableEntity rootTable = metaTableRepository.findById(tableId).orElse(null);

        LineageGraphRes res = new LineageGraphRes();
        res.setRootTableId(tableId);
        String rootName = rootTable != null ? rootTable.getTableName() : tableId;
        res.setRootTableName(rootName);

        Map<String, LineageGraphRes.GraphNode> nodeMap = new LinkedHashMap<>();
        List<LineageGraphRes.GraphLink> links = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        // Add root node
        if (rootTable != null) {
            addTableNode(nodeMap, rootTable, "root", 0);
        } else {
            LineageGraphRes.GraphNode rootNode = new LineageGraphRes.GraphNode();
            rootNode.setId(tableId);
            rootNode.setName(rootName);
            rootNode.setCategory("root");
            rootNode.setDepth(0);
            rootNode.setSymbolSize("40");
            nodeMap.put(tableId, rootNode);
        }

        // Traverse upstream
        int[] upstreamCount = {0};
        traverseUpstreamGraph(tableId, nodeMap, links, visited, 1, maxDepth, upstreamCount, rootName);

        // Traverse downstream
        visited.clear();
        int[] downstreamCount = {0};
        traverseDownstreamGraph(tableId, nodeMap, links, visited, 1, maxDepth, downstreamCount, rootName);

        // Compute max depth
        int maxFound = 0;
        for (LineageGraphRes.GraphNode node : nodeMap.values()) {
            if (node.getDepth() != null && node.getDepth() > maxFound) {
                maxFound = node.getDepth();
            }
            // Assign layer for layered layout
            if ("root".equals(node.getCategory())) {
                node.setLayer(0);
            } else if ("upstream".equals(node.getCategory())) {
                node.setLayer(-node.getDepth());
            } else {
                node.setLayer(node.getDepth());
            }
        }

        res.setNodes(new ArrayList<>(nodeMap.values()));
        res.setLinks(links);
        res.setTotalUpstream(upstreamCount[0]);
        res.setTotalDownstream(downstreamCount[0]);
        res.setMaxDepth(maxFound);
        return res;
    }

    private void traverseUpstreamGraph(String tableId, Map<String, LineageGraphRes.GraphNode> nodeMap,
                                        List<LineageGraphRes.GraphLink> links, Set<String> visited,
                                        int depth, int maxDepth, int[] count, String viaTable) {
        if (depth > maxDepth || visited.contains(tableId)) return;
        visited.add(tableId);

        List<MetaLineageEntity> lineages = metaLineageRepository.findByTargetTableId(tableId);
        for (MetaLineageEntity l : lineages) {
            count[0]++;

            MetaTableEntity sourceTable = metaTableRepository.findById(l.getSourceTableId()).orElse(null);
            if (sourceTable != null) {
                addTableNode(nodeMap, sourceTable, "upstream", depth);
            } else {
                // External source
                if (!nodeMap.containsKey(l.getSourceTableId())) {
                    LineageGraphRes.GraphNode node = new LineageGraphRes.GraphNode();
                    node.setId(l.getSourceTableId());
                    node.setName(l.getSourceTableName());
                    node.setCategory("external");
                    node.setDepth(depth);
                    node.setLayer(-depth);
                    node.setSymbolSize("30");
                    nodeMap.put(l.getSourceTableId(), node);
                }
            }

            LineageGraphRes.GraphLink link = new LineageGraphRes.GraphLink();
            link.setSource(l.getSourceTableId());
            link.setTarget(l.getTargetTableId());
            link.setRelationType(l.getRelationType());
            link.setRelationName(l.getRelationName());
            link.setTransformationSql(l.getTransformationSql());
            link.setDescription(l.getDescription());
            link.setFieldLinks(buildFieldLinks(l));
            links.add(link);

            traverseUpstreamGraph(l.getSourceTableId(), nodeMap, links, visited, depth + 1, maxDepth, count, l.getSourceTableName());
        }
    }

    private void traverseDownstreamGraph(String tableId, Map<String, LineageGraphRes.GraphNode> nodeMap,
                                          List<LineageGraphRes.GraphLink> links, Set<String> visited,
                                          int depth, int maxDepth, int[] count, String viaTable) {
        if (depth > maxDepth || visited.contains(tableId)) return;
        visited.add(tableId);

        List<MetaLineageEntity> lineages = metaLineageRepository.findBySourceTableId(tableId);
        for (MetaLineageEntity l : lineages) {
            count[0]++;

            MetaTableEntity targetTable = metaTableRepository.findById(l.getTargetTableId()).orElse(null);
            if (targetTable != null) {
                addTableNode(nodeMap, targetTable, "downstream", depth);
            } else {
                if (!nodeMap.containsKey(l.getTargetTableId())) {
                    LineageGraphRes.GraphNode node = new LineageGraphRes.GraphNode();
                    node.setId(l.getTargetTableId());
                    node.setName(l.getTargetTableName());
                    node.setCategory("external");
                    node.setDepth(depth);
                    node.setLayer(depth);
                    node.setSymbolSize("30");
                    nodeMap.put(l.getTargetTableId(), node);
                }
            }

            LineageGraphRes.GraphLink link = new LineageGraphRes.GraphLink();
            link.setSource(l.getSourceTableId());
            link.setTarget(l.getTargetTableId());
            link.setRelationType(l.getRelationType());
            link.setRelationName(l.getRelationName());
            link.setTransformationSql(l.getTransformationSql());
            link.setDescription(l.getDescription());
            link.setFieldLinks(buildFieldLinks(l));
            links.add(link);

            traverseDownstreamGraph(l.getTargetTableId(), nodeMap, links, visited, depth + 1, maxDepth, count, l.getTargetTableName());
        }
    }

    private void addTableNode(Map<String, LineageGraphRes.GraphNode> nodeMap, MetaTableEntity table,
                               String category, int depth) {
        if (nodeMap.containsKey(table.getId())) return;

        LineageGraphRes.GraphNode node = new LineageGraphRes.GraphNode();
        node.setId(table.getId());
        node.setName(table.getTableName());
        node.setCategory(category);
        node.setDepth(depth);
        node.setRowCount(table.getRowCount());
        // Compute column count
        long colCount = metaColumnRepository.countByTableId(table.getId());
        node.setColumnCount(colCount);
        // Symbol size based on column count
        node.setSymbolSize(colCount > 20 ? "50" : colCount > 10 ? "40" : "30");

        // Lookup datasource name
        datasourceRepository.findById(table.getDatasourceId())
                .ifPresent(ds -> node.setDatasourceName(ds.getName()));

        nodeMap.put(table.getId(), node);
    }

    private List<LineageGraphRes.FieldLink> buildFieldLinks(MetaLineageEntity l) {
        List<MetaFieldLineageEntity> fieldLineages = metaFieldLineageRepository
                .findBySourceTableIdAndTargetTableId(l.getSourceTableId(), l.getTargetTableId());
        // Remove duplicates by (sourceColumn, targetColumn)
        Map<String, MetaFieldLineageEntity> dedup = new LinkedHashMap<>();
        for (MetaFieldLineageEntity fl : fieldLineages) {
            String k = fl.getSourceColumnName() + "|" + fl.getTargetColumnName();
            dedup.putIfAbsent(k, fl);
        }
        if (dedup.isEmpty()) return null;

        return dedup.values().stream().map(fl -> {
            LineageGraphRes.FieldLink flink = new LineageGraphRes.FieldLink();
            flink.setSourceColumn(fl.getSourceColumnName());
            flink.setTargetColumn(fl.getTargetColumnName());
            flink.setTransformationLogic(fl.getTransformationLogic());
            return flink;
        }).collect(Collectors.toList());
    }

    // ==================== SQL Lineage Analysis ====================

    /**
     * Analyze a SQL statement and produce lineage (tables + field mappings).
     * Real tables are linked to registered metadata when available.
     */
    public SqlLineageRes analyzeSql(String sql, String tenantId) {
        SqlLineageRes parsed = sqlLineageParser.parse(sql);
        if (parsed == null) {
            throw new BusinessException("SQL 解析失败，暂不支持该语句类型");
        }

        // Build nodes: output + inputs
        List<SqlLineageRes.TableNode> nodes = new ArrayList<>();
        List<SqlLineageRes.TableEdge> edges = new ArrayList<>();

        SqlLineageRes.TableNode output = buildSqlNode(parsed.getOutputTable(), tenantId);
        output.setCategory("output");
        nodes.add(output);

        for (String inputName : parsed.getInputTables()) {
            SqlLineageRes.TableNode input = buildSqlNode(inputName, tenantId);
            input.setCategory("input");
            nodes.add(input);

            SqlLineageRes.TableEdge edge = new SqlLineageRes.TableEdge();
            edge.setSource(input.getId());
            edge.setTarget(output.getId());
            edge.setRelationType("SQL");
            edge.setRelationName(parsed.getSqlType());
            edge.setFieldMappings(parsed.getFieldMappings().stream()
                    .filter(fm -> inputName.equalsIgnoreCase(fm.getSourceTable()))
                    .collect(Collectors.toList()));
            edges.add(edge);
        }

        parsed.setNodes(nodes);
        parsed.setEdges(edges);
        return parsed;
    }

    private SqlLineageRes.TableNode buildSqlNode(String tableName, String tenantId) {
        SqlLineageRes.TableNode node = new SqlLineageRes.TableNode();
        node.setName(tableName);
        if (tableName == null) {
            node.setId("SQL_UNKNOWN");
            return node;
        }
        // Look up registered meta table
        List<MetaTableEntity> tables = metaTableRepository.findByTenantId(tenantId);
        Optional<MetaTableEntity> match = tables.stream()
                .filter(t -> t.getTableName().equalsIgnoreCase(tableName))
                .findFirst();
        if (match.isPresent()) {
            node.setId(match.get().getId());
            node.setRegistered(true);
        } else {
            node.setId("SQL_" + tableName);
            node.setRegistered(false);
        }
        return node;
    }

    // ==================== Graph Analysis ====================

    /**
     * Analyze the lineage graph around a table: cycle detection, depth stats, fan-out.
     */
    public LineageAnalysisRes analyzeGraph(String tableId, int maxDepth) {
        LineageGraphRes graph = buildLineageGraph(tableId, maxDepth);

        LineageAnalysisRes res = new LineageAnalysisRes();
        res.setTableName(graph.getRootTableName());
        res.setTotalNodes(graph.getNodes().size());
        res.setTotalEdges(graph.getLinks().size());
        res.setMaxDepth(graph.getMaxDepth());

        // Average depth of non-root nodes
        double sumDepth = 0;
        int countDepth = 0;
        for (LineageGraphRes.GraphNode n : graph.getNodes()) {
            if (n.getDepth() != null && n.getDepth() > 0) {
                sumDepth += n.getDepth();
                countDepth++;
            }
        }
        res.setAvgDepth(countDepth > 0 ? Math.round(sumDepth / countDepth * 10.0) / 10.0 : 0);

        // Max fan-out (out degree)
        Map<String, Integer> outDegree = new HashMap<>();
        for (LineageGraphRes.GraphLink l : graph.getLinks()) {
            outDegree.merge(l.getSource(), 1, Integer::sum);
        }
        res.setMaxFanOut(outDegree.values().stream().mapToInt(Integer::intValue).max().orElse(0));

        // Cycle detection (DFS three-color)
        Map<String, List<String>> adj = new HashMap<>();
        for (LineageGraphRes.GraphLink l : graph.getLinks()) {
            adj.computeIfAbsent(l.getSource(), k -> new ArrayList<>()).add(l.getTarget());
        }
        Set<String> white = new LinkedHashSet<>();
        Set<String> gray = new LinkedHashSet<>();
        Set<String> black = new LinkedHashSet<>();
        for (LineageGraphRes.GraphNode n : graph.getNodes()) {
            white.add(n.getId());
        }
        List<String> cyclePath = new ArrayList<>();
        boolean[] found = {false};
        for (String id : new ArrayList<>(white)) {
            if (white.contains(id)) {
                dfsCycle(id, adj, white, gray, black, new ArrayList<>(), cyclePath, found);
                if (found[0]) break;
            }
        }
        res.setHasCycle(found[0]);
        res.setCycleNodes(cyclePath);

        res.setSummary(buildAnalysisSummary(res, graph));
        return res;
    }

    private void dfsCycle(String node, Map<String, List<String>> adj,
                          Set<String> white, Set<String> gray, Set<String> black,
                          List<String> stack, List<String> cyclePath, boolean[] found) {
        white.remove(node);
        gray.add(node);
        stack.add(node);

        for (String next : adj.getOrDefault(node, List.of())) {
            if (found[0]) return;
            if (gray.contains(next)) {
                // cycle: from index of next in stack to end
                int idx = stack.indexOf(next);
                if (idx >= 0) {
                    cyclePath.addAll(stack.subList(idx, stack.size()));
                    cyclePath.add(next);
                }
                found[0] = true;
                return;
            }
            if (white.contains(next)) {
                dfsCycle(next, adj, white, gray, black, stack, cyclePath, found);
            }
        }
        gray.remove(node);
        black.add(node);
        stack.remove(stack.size() - 1);
    }

    private String buildAnalysisSummary(LineageAnalysisRes res, LineageGraphRes graph) {
        StringBuilder sb = new StringBuilder();
        sb.append("血缘图共 ").append(res.getTotalNodes()).append(" 个节点、")
                .append(res.getTotalEdges()).append(" 条边");
        if (res.getMaxDepth() > 0) {
            sb.append("，最远血缘深度 ").append(res.getMaxDepth())
                    .append("，平均深度 ").append(res.getAvgDepth());
        }
        sb.append("，最大扇出 ").append(res.getMaxFanOut());
        if (res.isHasCycle()) {
            sb.append("；⚠ 检测到血缘环：").append(String.join(" → ", res.getCycleNodes()));
        } else {
            sb.append("；未检测到血缘环，链路健康");
        }
        return sb.toString();
    }

    // ==================== Impact Analysis ====================

    /**
     * Analyze downstream impact: what tables are affected if this table changes.
     * Returns impact paths ordered by depth.
     */
    public ImpactAnalysisRes analyzeImpact(String tableId, int maxDepth) {
        MetaTableEntity table = metaTableRepository.findById(tableId).orElse(null);

        ImpactAnalysisRes res = new ImpactAnalysisRes();
        res.setTableId(tableId);
        String tableName = table != null ? table.getTableName() : tableId;
        res.setTableName(tableName);

        List<ImpactAnalysisRes.ImpactPath> paths = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        visited.add(tableId);

        collectImpactPaths(tableId, paths, new ArrayList<>(), visited, 1, maxDepth, tableName);

        // Sort by depth
        paths.sort(Comparator.comparingInt(ImpactAnalysisRes.ImpactPath::getDepth));

        int maxDepthFound = paths.stream().mapToInt(ImpactAnalysisRes.ImpactPath::getDepth).max().orElse(0);

        res.setPaths(paths);
        res.setTotalImpacted(paths.size());
        res.setMaxDepth(maxDepthFound);
        return res;
    }

    private void collectImpactPaths(String tableId, List<ImpactAnalysisRes.ImpactPath> paths,
                                     List<String> chain, Set<String> visited, int depth, int maxDepth,
                                     String currentName) {
        if (depth > maxDepth) return;

        List<String> newChain = new ArrayList<>(chain);
        newChain.add(currentName);

        List<MetaLineageEntity> lineages = metaLineageRepository.findBySourceTableId(tableId);
        for (MetaLineageEntity l : lineages) {
            if (visited.contains(l.getTargetTableId())) continue;
            visited.add(l.getTargetTableId());

            ImpactAnalysisRes.ImpactPath path = new ImpactAnalysisRes.ImpactPath();
            path.setTableId(l.getTargetTableId());
            path.setTableName(l.getTargetTableName());
            path.setDepth(depth);
            path.setRelationType(l.getRelationType());
            path.setRelationName(l.getRelationName());

            List<String> fullChain = new ArrayList<>(newChain);
            fullChain.add(l.getTargetTableName());
            path.setPathChain(fullChain);

            // Lookup datasource
            metaTableRepository.findById(l.getTargetTableId())
                    .ifPresent(t -> datasourceRepository.findById(t.getDatasourceId())
                            .ifPresent(ds -> path.setDatasourceName(ds.getName())));

            paths.add(path);

            collectImpactPaths(l.getTargetTableId(), paths, newChain, visited, depth + 1, maxDepth, l.getTargetTableName());
        }
    }

    // ==================== Field-level Lineage ====================

    /**
     * Get field-level lineage for a specific column.
     */
    public List<LineageGraphRes.FieldLink> getFieldLineage(String tableId, String columnName) {
        List<MetaFieldLineageEntity> upstream = metaFieldLineageRepository
                .findByTargetTableIdAndTargetColumnName(tableId, columnName);
        List<MetaFieldLineageEntity> downstream = metaFieldLineageRepository
                .findBySourceTableIdAndSourceColumnName(tableId, columnName);

        List<LineageGraphRes.FieldLink> result = new ArrayList<>();

        for (MetaFieldLineageEntity fl : upstream) {
            LineageGraphRes.FieldLink link = new LineageGraphRes.FieldLink();
            link.setSourceColumn(fl.getSourceColumnName());
            link.setTargetColumn(fl.getTargetColumnName());
            link.setTransformationLogic(fl.getTransformationLogic());
            result.add(link);
        }
        for (MetaFieldLineageEntity fl : downstream) {
            LineageGraphRes.FieldLink link = new LineageGraphRes.FieldLink();
            link.setSourceColumn(fl.getSourceColumnName());
            link.setTargetColumn(fl.getTargetColumnName());
            link.setTransformationLogic(fl.getTransformationLogic());
            result.add(link);
        }

        return result;
    }

    // ==================== Search ====================

    /**
     * Search tables with lineage information by keyword.
     */
    public List<MetaTableRes> searchLineage(String keyword, String tenantId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return new ArrayList<>();
        }

        String kw = keyword.trim().toLowerCase();

        // Search tables by name
        List<MetaTableEntity> tables = metaTableRepository.findByTenantId(tenantId);
        List<MetaTableEntity> matched = tables.stream()
                .filter(t -> t.getTableName().toLowerCase().contains(kw)
                        || (t.getComment() != null && t.getComment().toLowerCase().contains(kw)))
                .collect(Collectors.toList());

        List<MetaTableRes> result = new ArrayList<>();
        for (MetaTableEntity table : matched) {
            MetaTableRes res = toTableRes(table);

            // Check if table has lineage
            boolean hasLineage = !metaLineageRepository.findBySourceTableIdOrTargetTableId(
                    table.getId(), table.getId()).isEmpty();
            res.setHasLineage(hasLineage);

            // Count upstream / downstream
            long upstreamCount = metaLineageRepository.findByTargetTableId(table.getId()).size();
            long downstreamCount = metaLineageRepository.findBySourceTableId(table.getId()).size();
            res.setUpstreamCount((int) upstreamCount);
            res.setDownstreamCount((int) downstreamCount);

            result.add(res);
        }

        return result;
    }

    // ==================== Manual Lineage Management ====================

    /**
     * Add a manual table-level lineage relationship.
     */
    @Transactional
    public void addLineage(String sourceTableId, String targetTableId, String relationType,
                            String relationName, String transformationSql, String description,
                            String tenantId) {
        MetaTableEntity source = metaTableRepository.findById(sourceTableId).orElse(null);
        MetaTableEntity target = metaTableRepository.findById(targetTableId).orElse(null);

        MetaLineageEntity entity = new MetaLineageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setSourceTableId(sourceTableId);
        entity.setSourceTableName(source != null ? source.getTableName() : sourceTableId);
        entity.setTargetTableId(targetTableId);
        entity.setTargetTableName(target != null ? target.getTableName() : targetTableId);
        entity.setRelationType(relationType != null ? relationType : "MANUAL");
        entity.setRelationName(relationName);
        entity.setTransformationSql(transformationSql);
        entity.setDescription(description);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        metaLineageRepository.save(entity);

        log.info("Manual lineage added: {} -> {} ({})", sourceTableId, targetTableId, relationType);
    }

    /**
     * Add field-level lineage to an existing table-level lineage.
     */
    @Transactional
    public void addFieldLineage(String lineageId, String sourceColumn, String targetColumn,
                                 String transformationLogic, String comment) {
        MetaLineageEntity lineage = null;
        if (lineageId != null && lineageId.contains("|")) {
            String[] parts = lineageId.split("\\|", 2);
            List<MetaLineageEntity> candidates = metaLineageRepository
                    .findBySourceTableIdAndTargetTableId(parts[0], parts[1]);
            if (!candidates.isEmpty()) {
                lineage = candidates.get(0);
            }
        } else if (lineageId != null && !lineageId.isEmpty()) {
            lineage = metaLineageRepository.findById(lineageId).orElse(null);
        }
        if (lineage == null) {
            throw new BusinessException("血缘关系不存在");
        }

        MetaFieldLineageEntity entity = new MetaFieldLineageEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setLineageId(lineageId);
        entity.setSourceTableId(lineage.getSourceTableId());
        entity.setSourceColumnName(sourceColumn);
        entity.setTargetTableId(lineage.getTargetTableId());
        entity.setTargetColumnName(targetColumn);
        entity.setTransformationLogic(transformationLogic);
        entity.setComment(comment);
        entity.setCreateTime(LocalDateTime.now());
        metaFieldLineageRepository.save(entity);

        log.info("Field lineage added: {}.{} -> {}.{}", lineage.getSourceTableName(), sourceColumn,
                lineage.getTargetTableName(), targetColumn);
    }

    /**
     * Delete a lineage relationship and its field-level lineage.
     */
    @Transactional
    public void deleteLineage(String lineageId) {
        metaFieldLineageRepository.deleteByLineageId(lineageId);
        metaLineageRepository.deleteById(lineageId);
        log.info("Lineage deleted: {}", lineageId);
    }

    /**
     * List all lineage relationships for a tenant.
     */
    public List<MetaLineageRes> listAllLineage(String tenantId) {
        List<MetaLineageEntity> entities = metaLineageRepository.findByTenantId(tenantId);
        return entities.stream()
                .map(e -> {
                    MetaLineageRes res = new MetaLineageRes();
                    MetaLineageRes.LineageNode node = new MetaLineageRes.LineageNode();
                    node.setTableId(e.getSourceTableId());
                    node.setTableName(e.getSourceTableName());
                    node.setRelationType(e.getRelationType());
                    node.setRelationName(e.getRelationName());
                    node.setDepth(1);
                    res.setTableId(e.getTargetTableId());
                    res.setTableName(e.getTargetTableName());
                    res.setUpstream(List.of(node));
                    // Build edges
                    MetaLineageRes.LineageEdge edge = new MetaLineageRes.LineageEdge();
                    edge.setId(e.getId());
                    edge.setSourceTableId(e.getSourceTableId());
                    edge.setSourceTableName(e.getSourceTableName());
                    edge.setTargetTableId(e.getTargetTableId());
                    edge.setTargetTableName(e.getTargetTableName());
                    edge.setRelationType(e.getRelationType());
                    edge.setRelationName(e.getRelationName());
                    res.setEdges(List.of(edge));
                    return res;
                })
                .collect(Collectors.toList());
    }

    // ==================== Helper ====================

    private MetaTableRes toTableRes(MetaTableEntity entity) {
        MetaTableRes res = new MetaTableRes();
        res.setId(entity.getId());
        res.setDatasourceId(entity.getDatasourceId());
        res.setSchemaName(entity.getSchemaName());
        res.setTableName(entity.getTableName());
        res.setComment(entity.getComment());
        res.setRowCount(entity.getRowCount());
        res.setDataSize(entity.getDataSize());
        res.setSyncTime(entity.getSyncTime());
        res.setCreateTime(entity.getCreateTime());
        datasourceRepository.findById(entity.getDatasourceId())
                .ifPresent(ds -> res.setDatasourceName(ds.getName()));
        return res;
    }
}