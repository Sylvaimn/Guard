package com.temporedata.guard.modules.common.cluster.repository;

import com.temporedata.guard.modules.common.cluster.entity.ClusterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterRepository extends JpaRepository<ClusterEntity, String> {

    List<ClusterEntity> findByTenantId(String tenantId);

    long countByStatus(String status);
}