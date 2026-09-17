package com.temporedata.guard.modules.dev.catalog.service;
import com.temporedata.guard.modules.security.changeaudit.service.ChangeAuditService;
import com.temporedata.guard.modules.dev.catalog.entity.ColumnMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.ColumnMetadataRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaColumnEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaColumnRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaLineageEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaLineageRepository;
import com.temporedata.guard.modules.dev.meta.entity.MetaTableEntity;
import com.temporedata.guard.modules.dev.meta.repository.MetaTableRepository;
import com.temporedata.guard.modules.dev.catalog.entity.TableMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.catalog.*;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.ingestion.entity.IngestionTaskEntity;
import com.temporedata.guard.modules.dev.ingestion.repository.IngestionTaskRepository;
import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Data catalog service: metadata sync, browse, comment editing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CatalogService {

    @Autowired
    @Lazy
    private CatalogService self;

    private final TableMetadataRepository tableMetadataRepository;
    private final ColumnMetadataRepository columnMetadataRepository;
    private final DatasourceRepository datasourceRepository;
    private final JdbcConnectionManager connectionManager;
    private final SyncTaskRepository syncTaskRepository;
    private final IngestionTaskRepository ingestionTaskRepository;
    private final QualityRuleRepository qualityRuleRepository;
    private final ChangeAuditService changeAuditService;
    private final MetaTableRepository metaTableRepository;
    private final MetaColumnRepository metaColumnRepository;
    private final MetaLineageRepository metaLineageRepository;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    public List<TableMetadataRes> list(String tenantId) {
        return tableMetadataRepository.findByTenantIdOrderBySyncTimeDesc(tenantId).stream()
                .map(this::toTableRes)
                .collect(Collectors.toList());
    }

    public TableMetadataRes detail(String id) {
        TableMetadataEntity table = tableMetadataRepository.findById(id)
                .orElseThrow(() -> new BusinessException("表元数据不存在"));
        return toTableResWithColumns(table);
    }

    /**
     * Sync metadata from datasource's information_schema.
     * Uses upsert semantics: existing tables/columns are updated in place, manual
     * comments are preserved (detected via syncedComment), removed tables/columns
     * are dropped, and an empty source never wipes existing catalog data.
     */
    @Transactional
    public Map<String, Object> sync(String datasourceId) {
        DatasourceEntity ds = datasourceRepository.findById(datasourceId)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        String plainPwd = CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        javax.sql.DataSource dataSource = connectionManager.getOrCreate(ds, plainPwd);

        Map<String, Object> result = new LinkedHashMap<>();
        int tableCount = 0;
        int columnCount = 0;

        try (Connection conn = dataSource.getConnection()) {
            String schemaName = "PUBLIC";

            // Query tables (exclude platform internal zy_* tables), H2/MySQL compatible
            String tableSql = "SELECT TABLE_NAME, REMARKS FROM INFORMATION_SCHEMA.TABLES " +
                    "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE IN ('BASE TABLE', 'TABLE') " +
                    "AND UPPER(TABLE_NAME) NOT LIKE 'ZY\\_%' ESCAPE '\\'";
            List<Map<String, String>> tables = new ArrayList<>();
            try (var ps = conn.prepareStatement(tableSql)) {
                ps.setString(1, schemaName);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, String> row = new HashMap<>();
                        row.put("tableName", rs.getString("TABLE_NAME"));
                        row.put("comment", rs.getString("REMARKS"));
                        tables.add(row);
                    }
                }
            }

            // Empty source: protect existing catalog data (e.g. demo H2 restarted)
            if (tables.isEmpty()) {
                result.put("skipped", true);
                result.put("reason", "数据源中没有业务表，保留现有目录数据");
                return result;
            }

            // Existing metadata for this datasource
            List<TableMetadataEntity> existing = tableMetadataRepository.findByDatasourceId(datasourceId);
            Map<String, TableMetadataEntity> existingByName = existing.stream()
                    .collect(Collectors.toMap(t -> (t.getSchemaName() != null ? t.getSchemaName() : "") + "." + t.getTableName().toUpperCase(),
                            t -> t, (a, b) -> a));

            Set<String> keptTableKeys = new HashSet<>();
            for (Map<String, String> t : tables) {
                String tableName = t.get("tableName");
                String comment = t.get("comment");
                String key = schemaName + "." + tableName.toUpperCase();
                keptTableKeys.add(key);

                TableMetadataEntity tableMeta = existingByName.get(key);
                boolean isNew = tableMeta == null;
                if (isNew) {
                    tableMeta = new TableMetadataEntity();
                    tableMeta.setId(UUID.randomUUID().toString().replace("-", ""));
                    tableMeta.setDatasourceId(datasourceId);
                    tableMeta.setSchemaName(schemaName);
                    tableMeta.setTableName(tableName);
                    tableMeta.setTenantId(ds.getTenantId());
                }
                tableMeta.setComment(preserveManualComment(tableMeta.getComment(), tableMeta.getSyncedComment(), comment));
                tableMeta.setSyncedComment(comment);
                tableMeta.setSyncTime(LocalDateTime.now());
                tableMetadataRepository.save(tableMeta);
                tableCount++;

                // Query columns (H2/MySQL compatible: no ORDINAL_POSITION in H2's information_schema)
                String colSql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, NUMERIC_PRECISION, " +
                        "NUMERIC_SCALE, IS_NULLABLE, COLUMN_DEFAULT, REMARKS " +
                        "FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";
                Set<String> pkColumns = new HashSet<>();
                String pkSql = "SELECT DISTINCT c.COLUMN_NAME FROM INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE c " +
                        "JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc ON c.CONSTRAINT_NAME = tc.CONSTRAINT_NAME " +
                        "WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ? AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY'";
                try (var ps = conn.prepareStatement(pkSql)) {
                    ps.setString(1, schemaName);
                    ps.setString(2, tableName);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            pkColumns.add(rs.getString("COLUMN_NAME"));
                        }
                    }
                } catch (Exception ignored) {}

                List<ColumnMetadataEntity> existingCols =
                        columnMetadataRepository.findByTableMetadataId(tableMeta.getId());
                Map<String, ColumnMetadataEntity> existingColsByName = existingCols.stream()
                        .collect(Collectors.toMap(c -> c.getColumnName().toUpperCase(), c -> c, (a, b) -> a));
                Set<String> keptColKeys = new HashSet<>();

                try (var ps = conn.prepareStatement(colSql)) {
                    ps.setString(1, schemaName);
                    ps.setString(2, tableName);
                    int ordinal = 0;
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            ordinal++;
                            String columnName = rs.getString("COLUMN_NAME");
                            keptColKeys.add(columnName.toUpperCase());
                            ColumnMetadataEntity col = existingColsByName.get(columnName.toUpperCase());
                            boolean colNew = col == null;
                            if (colNew) {
                                col = new ColumnMetadataEntity();
                                col.setId(UUID.randomUUID().toString().replace("-", ""));
                                col.setTableMetadataId(tableMeta.getId());
                                col.setColumnName(columnName);
                            }
                            String colComment = rs.getString("REMARKS");
                            col.setDataType(rs.getString("DATA_TYPE"));
                            Integer charLen = toInteger(rs.getObject("CHARACTER_MAXIMUM_LENGTH"));
                            Integer numPrec = toInteger(rs.getObject("NUMERIC_PRECISION"));
                            col.setColumnSize(charLen != null ? charLen : numPrec);
                            Integer numScale = toInteger(rs.getObject("NUMERIC_SCALE"));
                            col.setDecimalDigits(numScale);
                            col.setDefaultValue(rs.getString("COLUMN_DEFAULT"));
                            col.setOrdinalPosition(ordinal);
                            col.setIsNullable("YES".equalsIgnoreCase(rs.getString("IS_NULLABLE")));
                            col.setIsPrimaryKey(pkColumns.contains(columnName));
                            col.setComment(preserveManualComment(col.getComment(), col.getSyncedComment(), colComment));
                            col.setSyncedComment(colComment);
                            columnMetadataRepository.save(col);
                            columnCount++;
                        }
                    }
                }

                // Drop columns that no longer exist in the source
                for (Map.Entry<String, ColumnMetadataEntity> e : existingColsByName.entrySet()) {
                    if (!keptColKeys.contains(e.getKey())) {
                        columnMetadataRepository.delete(e.getValue());
                    }
                }

                // Update row count
                try (var ps = conn.prepareStatement("SELECT COUNT(*) FROM " + tableName)) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            tableMeta.setRowCount(rs.getLong(1));
                            tableMetadataRepository.save(tableMeta);
                        }
                    }
                }
            }

            // Drop tables that no longer exist in the source
            for (Map.Entry<String, TableMetadataEntity> e : existingByName.entrySet()) {
                if (!keptTableKeys.contains(e.getKey())) {
                    columnMetadataRepository.deleteByTableMetadataId(e.getValue().getId());
                    tableMetadataRepository.delete(e.getValue());
                }
            }

            result.put("skipped", false);
            result.put("tableCount", tableCount);
            result.put("columnCount", columnCount);
            return result;
        } catch (Exception e) {
            log.error("Metadata sync failed for datasource {}", datasourceId, e);
            throw new BusinessException("元数据同步失败: " + e.getMessage());
        }
    }

    /**
     * Keep a manually edited comment if it differs from the last synced value
     * (or was never synced); otherwise adopt the freshly collected comment.
     */
    private String preserveManualComment(String current, String synced, String collected) {
        if (current != null && (synced == null || !current.equals(synced))) {
            return current;
        }
        return collected;
    }

    /**
     * Migrate legacy meta tables (ZY_META_TABLE / ZY_META_COLUMN) into the catalog
     * metadata tables. Called once at startup when catalog data is empty.
     */
    public int migrateLegacyMeta() {
        List<MetaTableEntity> metaTables = metaTableRepository.findAll();
        if (metaTables.isEmpty()) {
            return 0;
        }
        int migrated = 0;
        for (MetaTableEntity mt : metaTables) {
            String schema = mt.getSchemaName() != null ? mt.getSchemaName() : "PUBLIC";
            TableMetadataEntity existing = tableMetadataRepository
                    .findByDatasourceIdAndSchemaNameAndTableName(mt.getDatasourceId(), schema, mt.getTableName());
            if (existing != null) {
                continue;
            }
            TableMetadataEntity tm = new TableMetadataEntity();
            tm.setId(UUID.randomUUID().toString().replace("-", ""));
            tm.setDatasourceId(mt.getDatasourceId());
            tm.setSchemaName(schema);
            tm.setTableName(mt.getTableName());
            tm.setComment(mt.getComment());
            tm.setSyncedComment(mt.getComment());
            tm.setRowCount(mt.getRowCount());
            tm.setTenantId(mt.getTenantId());
            tm.setSyncTime(mt.getSyncTime() != null ? mt.getSyncTime() : LocalDateTime.now());
            tableMetadataRepository.save(tm);

            List<MetaColumnEntity> cols = metaColumnRepository.findByTableIdOrderByOrdinalPositionAsc(mt.getId());
            for (MetaColumnEntity mc : cols) {
                ColumnMetadataEntity col = new ColumnMetadataEntity();
                col.setId(UUID.randomUUID().toString().replace("-", ""));
                col.setTableMetadataId(tm.getId());
                col.setColumnName(mc.getColumnName());
                col.setDataType(mc.getColumnType());
                col.setColumnSize(mc.getColumnSize());
                col.setDecimalDigits(mc.getDecimalDigits());
                col.setDefaultValue(mc.getDefaultValue());
                col.setOrdinalPosition(mc.getOrdinalPosition() != null ? mc.getOrdinalPosition() : 0);
                col.setIsNullable(mc.getNullable() != null ? mc.getNullable() : true);
                col.setIsPrimaryKey(mc.getPrimaryKey() != null ? mc.getPrimaryKey() : false);
                col.setComment(mc.getComment());
                col.setSyncedComment(mc.getComment());
                columnMetadataRepository.save(col);
            }
            migrated++;
        }
        log.info("Migrated {} tables from legacy meta", migrated);
        return migrated;
    }

    /**
     * Update table or column comment.
     */
    @Transactional
    public void updateComment(CommentUpdateReq req) {
        if (req.getColumnId() != null) {
            ColumnMetadataEntity col = columnMetadataRepository.findById(req.getColumnId())
                    .orElseThrow(() -> new BusinessException("列元数据不存在"));
            String before = col.getComment();
            col.setComment(req.getComment());
            columnMetadataRepository.save(col);
            String tenantId = tableMetadataRepository.findById(col.getTableMetadataId())
                    .map(TableMetadataEntity::getTenantId).orElse(null);
            changeAuditService.record("COLUMN", col.getId(), col.getColumnName(), "UPDATE",
                    null, "comment=" + before, "comment=" + req.getComment(), tenantId);
        } else if (req.getTableId() != null) {
            TableMetadataEntity table = tableMetadataRepository.findById(req.getTableId())
                    .orElseThrow(() -> new BusinessException("表元数据不存在"));
            String before = table.getComment();
            table.setComment(req.getComment());
            tableMetadataRepository.save(table);
            changeAuditService.record("TABLE", table.getId(), table.getTableName(), "UPDATE",
                    null, "comment=" + before, "comment=" + req.getComment(), table.getTenantId());
        } else {
            throw new BusinessException("请指定表ID或列ID");
        }
    }

    private TableMetadataRes toTableRes(TableMetadataEntity e) {
        TableMetadataRes res = new TableMetadataRes();
        res.setId(e.getId());
        res.setDatasourceId(e.getDatasourceId());
        res.setSchemaName(e.getSchemaName());
        res.setTableName(e.getTableName());
        res.setComment(e.getComment());
        res.setRowCount(e.getRowCount());
        res.setDataLevelId(e.getDataLevelId());
        res.setDataCategoryId(e.getDataCategoryId());
        res.setSyncTime(e.getSyncTime() != null ? e.getSyncTime().toString() : null);
        datasourceRepository.findById(e.getDatasourceId())
                .ifPresent(ds -> res.setDatasourceName(ds.getName()));
        return res;
    }

    private TableMetadataRes toTableResWithColumns(TableMetadataEntity e) {
        TableMetadataRes res = toTableRes(e);
        List<ColumnMetadataEntity> cols = columnMetadataRepository.findByTableMetadataIdOrderByOrdinalPosition(e.getId());
        res.setColumns(cols.stream().map(this::toColRes).collect(Collectors.toList()));
        return res;
    }

    private ColumnMetadataRes toColRes(ColumnMetadataEntity e) {
        ColumnMetadataRes res = new ColumnMetadataRes();
        res.setId(e.getId());
        res.setColumnName(e.getColumnName());
        res.setDataType(e.getDataType());
        res.setColumnSize(e.getColumnSize());
        res.setDecimalDigits(e.getDecimalDigits());
        res.setDefaultValue(e.getDefaultValue());
        res.setOrdinalPosition(e.getOrdinalPosition());
        res.setIsNullable(e.getIsNullable());
        res.setComment(e.getComment());
        res.setIsPrimaryKey(e.getIsPrimaryKey());
        res.setDataLevelId(e.getDataLevelId());
        return res;
    }

    /**
     * Update governance attributes (sensitivity level / category) of a table or column.
     */
    @Transactional
    public void updateGovernance(GovernanceUpdateReq req) {
        if (req.getColumnId() != null) {
            ColumnMetadataEntity col = columnMetadataRepository.findById(req.getColumnId())
                    .orElseThrow(() -> new BusinessException("列元数据不存在"));
            String before = "level=" + col.getDataLevelId();
            col.setDataLevelId(blankToNull(req.getDataLevelId()));
            columnMetadataRepository.save(col);
            String tenantId = tableMetadataRepository.findById(col.getTableMetadataId())
                    .map(TableMetadataEntity::getTenantId).orElse(null);
            changeAuditService.record("COLUMN", col.getId(), col.getColumnName(), "GOVERNANCE",
                    null, before, "level=" + col.getDataLevelId(), tenantId);
        } else if (req.getTableId() != null) {
            TableMetadataEntity table = tableMetadataRepository.findById(req.getTableId())
                    .orElseThrow(() -> new BusinessException("表元数据不存在"));
            String before = "level=" + table.getDataLevelId() + ",category=" + table.getDataCategoryId();
            table.setDataLevelId(blankToNull(req.getDataLevelId()));
            if (req.getDataCategoryId() != null) {
                table.setDataCategoryId(blankToNull(req.getDataCategoryId()));
            }
            tableMetadataRepository.save(table);
            changeAuditService.record("TABLE", table.getId(), table.getTableName(), "GOVERNANCE",
                    null, before,
                    "level=" + table.getDataLevelId() + ",category=" + table.getDataCategoryId(),
                    table.getTenantId());
        } else {
            throw new BusinessException("请指定表ID或列ID");
        }
    }

    private Integer toInteger(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number) {
            return ((Number) o).intValue();
        }
        try {
            return Integer.valueOf(o.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    // ===== P4: Enhanced metadata collection & catalog =====

    /**
     * Sync metadata from all datasources.
     */
    public Map<String, Object> syncAll() {
        List<DatasourceEntity> datasources = datasourceRepository.findAll();
        int success = 0;
        int fail = 0;
        List<String> errors = new ArrayList<>();

        for (DatasourceEntity ds : datasources) {
            try {
                self.sync(ds.getId());
                success++;
            } catch (Exception e) {
                fail++;
                errors.add(ds.getName() + ": " + e.getMessage());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", datasources.size());
        result.put("success", success);
        result.put("fail", fail);
        result.put("errors", errors);
        return result;
    }

    /**
     * Build catalog tree: datasource -> schema -> table.
     */
    public List<CatalogTreeRes> tree(String tenantId) {
        List<DatasourceEntity> datasources = datasourceRepository.findAll();
        List<CatalogTreeRes> tree = new ArrayList<>();

        for (DatasourceEntity ds : datasources) {
            CatalogTreeRes dsNode = new CatalogTreeRes();
            dsNode.setId(ds.getId());
            dsNode.setLabel(ds.getName());
            dsNode.setType("DATASOURCE");
            dsNode.setDatasourceName(ds.getName());

            List<TableMetadataEntity> tables = tableMetadataRepository.findByDatasourceId(ds.getId());
            // Group by schema
            Map<String, List<TableMetadataEntity>> schemaGroups = tables.stream()
                    .collect(Collectors.groupingBy(t -> t.getSchemaName() != null ? t.getSchemaName() : "default"));

            List<CatalogTreeRes> schemaChildren = new ArrayList<>();
            for (Map.Entry<String, List<TableMetadataEntity>> entry : schemaGroups.entrySet()) {
                CatalogTreeRes schemaNode = new CatalogTreeRes();
                schemaNode.setId(ds.getId() + "_" + entry.getKey());
                schemaNode.setLabel(entry.getKey());
                schemaNode.setType("SCHEMA");
                schemaNode.setSchemaName(entry.getKey());

                List<CatalogTreeRes> tableChildren = entry.getValue().stream().map(t -> {
                    CatalogTreeRes tableNode = new CatalogTreeRes();
                    tableNode.setId(t.getId());
                    tableNode.setLabel(t.getTableName());
                    tableNode.setType("TABLE");
                    tableNode.setTableName(t.getTableName());
                    tableNode.setRowCount(t.getRowCount());
                    tableNode.setComment(t.getComment());
                    tableNode.setIsLeaf(true);
                    return tableNode;
                }).collect(Collectors.toList());

                schemaNode.setChildren(tableChildren);
                schemaChildren.add(schemaNode);
            }
            dsNode.setChildren(schemaChildren);
            tree.add(dsNode);
        }
        return tree;
    }

    /**
     * Build table lineage: upstream/downstream from sync, ingestion, quality rules.
     */
    public LineageRes lineage(String tableId) {
        TableMetadataEntity table = tableMetadataRepository.findById(tableId)
                .orElseThrow(() -> new BusinessException("表元数据不存在"));

        LineageRes res = new LineageRes();
        res.setTableId(table.getId());
        res.setTableName(table.getTableName());

        List<LineageRes.LineageNode> upstream = new ArrayList<>();
        List<LineageRes.LineageNode> downstream = new ArrayList<>();

        // Find sync tasks where this table is the target (upstream)
        List<SyncTaskEntity> syncAsTarget = syncTaskRepository.findAll().stream()
                .filter(t -> table.getTableName().equalsIgnoreCase(t.getTargetTable()))
                .collect(Collectors.toList());
        for (SyncTaskEntity st : syncAsTarget) {
            TableMetadataEntity sourceTable = tableMetadataRepository
                    .findByDatasourceIdAndSchemaNameAndTableName(st.getSourceDatasourceId(), "PUBLIC", st.getSourceTable());
            LineageRes.LineageNode node = new LineageRes.LineageNode();
            node.setTableId(sourceTable != null ? sourceTable.getId() : null);
            node.setTableName(st.getSourceTable());
            node.setRelationType("SYNC");
            node.setRelationName(st.getName());
            upstream.add(node);
        }

        // Find sync tasks where this table is the source (downstream)
        List<SyncTaskEntity> syncAsSource = syncTaskRepository.findAll().stream()
                .filter(t -> table.getTableName().equalsIgnoreCase(t.getSourceTable()))
                .collect(Collectors.toList());
        for (SyncTaskEntity st : syncAsSource) {
            LineageRes.LineageNode node = new LineageRes.LineageNode();
            node.setTableName(st.getTargetTable());
            node.setRelationType("SYNC");
            node.setRelationName(st.getName());
            downstream.add(node);
        }

        // Find ingestion tasks where this table is the target (upstream)
        List<IngestionTaskEntity> ingestions = ingestionTaskRepository.findAll().stream()
                .filter(t -> table.getTableName().equalsIgnoreCase(t.getTargetTable()))
                .collect(Collectors.toList());
        for (IngestionTaskEntity it : ingestions) {
            LineageRes.LineageNode node = new LineageRes.LineageNode();
            node.setTableName("FILE_UPLOAD");
            node.setRelationType("INGESTION");
            node.setRelationName(it.getName());
            upstream.add(node);
        }

        // Find quality rules on this table (downstream)
        List<QualityRuleEntity> rules = qualityRuleRepository.findAll().stream()
                .filter(r -> table.getTableName().equalsIgnoreCase(r.getTableName()))
                .collect(Collectors.toList());
        for (QualityRuleEntity r : rules) {
            LineageRes.LineageNode node = new LineageRes.LineageNode();
            node.setTableName(table.getTableName());
            node.setRelationType("QUALITY");
            node.setRelationName(r.getName());
            downstream.add(node);
        }

        // Manual / legacy lineage relations (temporedata_guard_meta_lineage, matched by table name
        // since catalog table ids can change across re-syncs)
        for (MetaLineageEntity l : metaLineageRepository.findByTargetTableName(table.getTableName())) {
            LineageRes.LineageNode node = new LineageRes.LineageNode();
            node.setTableId(resolveTableIdByName(l.getSourceTableName()));
            node.setTableName(l.getSourceTableName());
            node.setRelationType(l.getRelationType());
            node.setRelationName(l.getRelationName());
            upstream.add(node);
        }
        for (MetaLineageEntity l : metaLineageRepository.findBySourceTableName(table.getTableName())) {
            LineageRes.LineageNode node = new LineageRes.LineageNode();
            node.setTableId(resolveTableIdByName(l.getTargetTableName()));
            node.setTableName(l.getTargetTableName());
            node.setRelationType(l.getRelationType());
            node.setRelationName(l.getRelationName());
            downstream.add(node);
        }

        res.setUpstream(upstream);
        res.setDownstream(downstream);
        return res;
    }

    private String resolveTableIdByName(String tableName) {
        List<TableMetadataEntity> matches = tableMetadataRepository.findByTableName(tableName);
        return matches.isEmpty() ? null : matches.get(0).getId();
    }
}