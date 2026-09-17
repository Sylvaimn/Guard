package com.temporedata.guard.modules.dev.catalog.repository;

import com.temporedata.guard.modules.dev.catalog.entity.TableMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TableMetadataRepository extends JpaRepository<TableMetadataEntity, String> {

    List<TableMetadataEntity> findByTenantIdOrderBySyncTimeDesc(String tenantId);

    List<TableMetadataEntity> findByDatasourceId(String datasourceId);

    TableMetadataEntity findByDatasourceIdAndSchemaNameAndTableName(String datasourceId, String schemaName, String tableName);

    List<TableMetadataEntity> findByTableName(String tableName);
}