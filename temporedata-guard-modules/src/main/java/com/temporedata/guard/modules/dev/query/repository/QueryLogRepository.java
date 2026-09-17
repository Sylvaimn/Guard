package com.temporedata.guard.modules.dev.query.repository;

import com.temporedata.guard.modules.dev.query.entity.QueryLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QueryLogRepository extends JpaRepository<QueryLogEntity, String> {

    List<QueryLogEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}