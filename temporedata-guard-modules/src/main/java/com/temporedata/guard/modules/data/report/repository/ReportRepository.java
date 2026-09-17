package com.temporedata.guard.modules.data.report.repository;

import com.temporedata.guard.modules.data.report.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<ReportEntity, String> {

    List<ReportEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}