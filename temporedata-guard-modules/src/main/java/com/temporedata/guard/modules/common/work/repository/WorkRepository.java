package com.temporedata.guard.modules.common.work.repository;

import com.temporedata.guard.modules.common.work.entity.WorkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkRepository extends JpaRepository<WorkEntity, String> {

    List<WorkEntity> findByWorkflowId(String workflowId);

    List<WorkEntity> findByWorkflowIdOrderByTopIndexAsc(String workflowId);

    List<WorkEntity> findByTenantId(String tenantId);

    List<WorkEntity> findByStatus(String status);

    long countByStatus(String status);
}