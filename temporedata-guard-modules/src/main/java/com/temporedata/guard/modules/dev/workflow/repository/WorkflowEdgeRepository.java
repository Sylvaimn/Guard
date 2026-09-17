package com.temporedata.guard.modules.dev.workflow.repository;

import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEdgeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowEdgeRepository extends JpaRepository<WorkflowEdgeEntity, String> {

    List<WorkflowEdgeEntity> findByWorkflowId(String workflowId);

    void deleteByWorkflowId(String workflowId);
}