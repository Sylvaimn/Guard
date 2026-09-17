package com.temporedata.guard.modules.dev.meta.repository;

import com.temporedata.guard.modules.dev.meta.entity.MetaTableEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface MetaTableRepository extends JpaRepository<MetaTableEntity, String> {

    List<MetaTableEntity> findByDatasourceId(String datasourceId);

    List<MetaTableEntity> findByTenantId(String tenantId);

    Optional<MetaTableEntity> findByDatasourceIdAndSchemaNameAndTableName(
            String datasourceId, String schemaName, String tableName);

    List<MetaTableEntity> findByDatasourceIdAndSchemaName(String datasourceId, String schemaName);

    void deleteByDatasourceId(String datasourceId);
}