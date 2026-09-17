package com.temporedata.guard.modules.dev.ingestion.repository;

import com.temporedata.guard.modules.dev.ingestion.entity.IngestionTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngestionTaskRepository extends JpaRepository<IngestionTaskEntity, String> {

    List<IngestionTaskEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}