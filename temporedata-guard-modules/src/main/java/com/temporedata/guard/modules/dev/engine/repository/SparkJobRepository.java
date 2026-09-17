package com.temporedata.guard.modules.dev.engine.repository;

import com.temporedata.guard.modules.dev.engine.entity.SparkJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SparkJobRepository extends JpaRepository<SparkJobEntity, String> {

    List<SparkJobEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}