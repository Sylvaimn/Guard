package com.temporedata.guard.modules.dev.sync.repository;

import com.temporedata.guard.modules.dev.sync.entity.SyncTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SyncTaskRepository extends JpaRepository<SyncTaskEntity, String> {

    List<SyncTaskEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    List<SyncTaskEntity> findByStatus(String status);

    long countByStatus(String status);
}