package com.temporedata.guard.modules.police.entitymaster.repository;

import com.temporedata.guard.modules.police.entitymaster.entity.EntityMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EntityMappingRepository extends JpaRepository<EntityMappingEntity, String> {

    List<EntityMappingEntity> findByEntityId(String entityId);

    List<EntityMappingEntity> findByEntityIdIn(Collection<String> entityIds);

    List<EntityMappingEntity> findBySourceRecordId(String sourceRecordId);

    /** 批量: 命中任一共用来源记录的所有映射(邻居发现) */
    List<EntityMappingEntity> findBySourceRecordIdIn(Collection<String> sourceRecordIds);
}
