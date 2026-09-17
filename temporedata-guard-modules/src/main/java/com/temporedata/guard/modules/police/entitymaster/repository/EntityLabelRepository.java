package com.temporedata.guard.modules.police.entitymaster.repository;

import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface EntityLabelRepository extends JpaRepository<EntityLabelEntity, String> {

    List<EntityLabelEntity> findByEntityId(String entityId);

    List<EntityLabelEntity> findByEntityIdIn(Collection<String> entityIds);

    List<EntityLabelEntity> findByLabelCodeAndTenantId(String labelCode, String tenantId);

    /** 案件标签批量匹配: 给定标签码集合, 返回同租户下所有挂该标签的实体 */
    List<EntityLabelEntity> findByLabelTypeAndLabelCodeInAndTenantId(String labelType, Collection<String> labelCodes, String tenantId);
}
