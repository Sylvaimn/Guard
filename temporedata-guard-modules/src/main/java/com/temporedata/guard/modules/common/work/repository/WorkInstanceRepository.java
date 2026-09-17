package com.temporedata.guard.modules.common.work.repository;

import com.temporedata.guard.modules.common.work.entity.WorkInstanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WorkInstanceRepository extends JpaRepository<WorkInstanceEntity, String> {

    List<WorkInstanceEntity> findByWorkIdOrderByCreateDateTimeDesc(String workId);

    List<WorkInstanceEntity> findByWorkflowInstanceId(String workflowInstanceId);

    long countByStatus(String status);
}