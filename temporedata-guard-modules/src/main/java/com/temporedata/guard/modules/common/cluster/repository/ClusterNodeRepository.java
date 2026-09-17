package com.temporedata.guard.modules.common.cluster.repository;

import com.temporedata.guard.modules.common.cluster.entity.ClusterNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClusterNodeRepository extends JpaRepository<ClusterNodeEntity, String> {

    List<ClusterNodeEntity> findByClusterId(String clusterId);
}