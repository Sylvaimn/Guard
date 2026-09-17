package com.temporedata.guard.modules.police.photo.repository;

import com.temporedata.guard.modules.police.photo.entity.CompareReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompareReportRepository extends JpaRepository<CompareReportEntity, String> {
    List<CompareReportEntity> findByTaskId(String taskId);
}
