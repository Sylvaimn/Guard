package com.temporedata.guard.modules.dev.lineage.repository;

import com.temporedata.guard.modules.dev.lineage.entity.LineageNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LineageNodeRepository extends JpaRepository<LineageNodeEntity, String> {

    List<LineageNodeEntity> findByTenantId(String tenantId);

    List<LineageNodeEntity> findByTenantIdAndNodeType(String tenantId, String nodeType);

    Optional<LineageNodeEntity> findByTenantIdAndNodeTypeAndName(String tenantId, String nodeType, String name);

    List<LineageNodeEntity> findByTenantIdAndNodeTypeAndNameIn(String tenantId, String nodeType, List<String> names);

    void deleteByTenantId(String tenantId);
}