package com.temporedata.guard.modules.dev.workflow.repository;

import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {

    List<WorkflowEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    List<WorkflowEntity> findByStatus(String status);

    long countByStatus(String status);
}