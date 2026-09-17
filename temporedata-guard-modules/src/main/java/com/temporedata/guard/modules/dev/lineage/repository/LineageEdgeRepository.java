package com.temporedata.guard.modules.dev.lineage.repository;

import com.temporedata.guard.modules.dev.lineage.entity.LineageEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineageEdgeRepository extends JpaRepository<LineageEdgeEntity, String> {

    List<LineageEdgeEntity> findByTenantId(String tenantId);

    List<LineageEdgeEntity> findByTenantIdAndSourceId(String tenantId, String sourceId);

    List<LineageEdgeEntity> findByTenantIdAndTargetId(String tenantId, String targetId);

    List<LineageEdgeEntity> findByTenantIdAndSourceIdOrTargetId(String tenantId, String sourceId, String targetId);

    List<LineageEdgeEntity> findByTenantIdAndEdgeType(String tenantId, String edgeType);

    List<LineageEdgeEntity> findByTenantIdAndTaskId(String tenantId, String taskId);

    void deleteByTenantIdAndTaskId(String tenantId, String taskId);

    void deleteByTenantIdAndTaskIdAndEdgeType(String tenantId, String taskId, String edgeType);

    @org.springframework.data.jpa.repository.Query("delete from LineageEdgeEntity e where e.tenantId = ?1 and e.taskId like ?2")
    void deleteByTenantIdAndTaskIdLike(String tenantId, String taskIdPattern);

    void deleteByTenantId(String tenantId);
}