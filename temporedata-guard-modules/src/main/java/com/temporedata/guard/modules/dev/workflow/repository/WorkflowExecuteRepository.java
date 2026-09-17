package com.temporedata.guard.modules.dev.workflow.repository;

import com.temporedata.guard.modules.dev.workflow.entity.WorkflowExecuteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowExecuteRepository extends JpaRepository<WorkflowExecuteEntity, String> {

    List<WorkflowExecuteEntity> findByWorkflowIdOrderByStartTimeDesc(String workflowId);

    List<WorkflowExecuteEntity> findByStatus(String status);

    long countByStatus(String status);
}