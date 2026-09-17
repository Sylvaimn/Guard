package com.temporedata.guard.modules.dev.workflow.repository;

import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkflowNodeRepository extends JpaRepository<WorkflowNodeEntity, String> {

    List<WorkflowNodeEntity> findByWorkflowIdOrderBySortOrderAsc(String workflowId);

    void deleteByWorkflowId(String workflowId);
}