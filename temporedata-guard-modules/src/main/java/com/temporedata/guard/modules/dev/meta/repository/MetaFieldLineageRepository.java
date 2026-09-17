package com.temporedata.guard.modules.dev.meta.repository;

import com.temporedata.guard.modules.dev.meta.entity.MetaFieldLineageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MetaFieldLineageRepository extends JpaRepository<MetaFieldLineageEntity, String> {

    List<MetaFieldLineageEntity> findByLineageId(String lineageId);

    List<MetaFieldLineageEntity> findBySourceTableId(String tableId);

    List<MetaFieldLineageEntity> findBySourceTableIdAndSourceColumnName(String tableId, String columnName);

    List<MetaFieldLineageEntity> findByTargetTableIdAndTargetColumnName(String tableId, String columnName);

    void deleteByLineageId(String lineageId);

    List<MetaFieldLineageEntity> findBySourceTableIdAndTargetTableId(String sourceTableId, String targetTableId);
}