package com.temporedata.guard.modules.common.container.repository;

import com.temporedata.guard.modules.common.container.entity.ContainerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ContainerRepository extends JpaRepository<ContainerEntity, String> {

    List<ContainerEntity> findByClusterId(String clusterId);

    List<ContainerEntity> findByClusterNodeId(String clusterNodeId);

    List<ContainerEntity> findByStatus(String status);

    List<ContainerEntity> findByTenantId(String tenantId);
}