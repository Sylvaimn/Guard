package com.temporedata.guard.modules.common.resource.repository;

import com.temporedata.guard.modules.common.resource.entity.ResourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResourceRepository extends JpaRepository<ResourceEntity, String> {

    List<ResourceEntity> findByTenantId(String tenantId);

    List<ResourceEntity> findByTypeAndTenantId(String type, String tenantId);
}