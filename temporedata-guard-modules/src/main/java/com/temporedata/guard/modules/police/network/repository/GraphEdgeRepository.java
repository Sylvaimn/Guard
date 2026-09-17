package com.temporedata.guard.modules.police.network.repository;

import com.temporedata.guard.modules.police.network.entity.GraphEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GraphEdgeRepository extends JpaRepository<GraphEdgeEntity, String> {

    List<GraphEdgeEntity> findBySourceEntityIdOrTargetEntityIdOrderByWeightDesc(
            String sourceEntityId, String targetEntityId);

    List<GraphEdgeEntity> findByRelationTypeAndTenantIdOrderByWeightDesc(String relationType, String tenantId);

    boolean existsBySourceEntityIdAndTargetEntityIdAndRelationType(
            String sourceEntityId, String targetEntityId, String relationType);
}
