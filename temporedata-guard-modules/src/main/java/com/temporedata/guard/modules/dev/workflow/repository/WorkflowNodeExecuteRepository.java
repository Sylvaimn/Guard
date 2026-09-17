package com.temporedata.guard.modules.dev.workflow.repository;

import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeExecuteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowNodeExecuteRepository extends JpaRepository<WorkflowNodeExecuteEntity, String> {

    List<WorkflowNodeExecuteEntity> findByExecuteId(String executeId);

    List<WorkflowNodeExecuteEntity> findByStatus(String status);
}