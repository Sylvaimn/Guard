package com.temporedata.guard.modules.dev.engine.repository;

import com.temporedata.guard.modules.dev.engine.entity.FlinkJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlinkJobRepository extends JpaRepository<FlinkJobEntity, String> {

    List<FlinkJobEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}