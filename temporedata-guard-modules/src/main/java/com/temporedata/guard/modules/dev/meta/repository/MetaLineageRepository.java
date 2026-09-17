package com.temporedata.guard.modules.dev.meta.repository;

import com.temporedata.guard.modules.dev.meta.entity.MetaLineageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MetaLineageRepository extends JpaRepository<MetaLineageEntity, String> {

    List<MetaLineageEntity> findBySourceTableId(String sourceTableId);

    List<MetaLineageEntity> findByTargetTableId(String targetTableId);

    List<MetaLineageEntity> findBySourceTableIdOrTargetTableId(String sourceTableId, String targetTableId);

    void deleteBySourceTableIdOrTargetTableId(String sourceTableId, String targetTableId);

    List<MetaLineageEntity> findByTenantId(String tenantId);

    List<MetaLineageEntity> findBySourceTableIdAndTargetTableId(String sourceTableId, String targetTableId);

    List<MetaLineageEntity> findBySourceTableName(String sourceTableName);

    List<MetaLineageEntity> findByTargetTableName(String targetTableName);
}