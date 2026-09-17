package com.temporedata.guard.modules.police.photo.repository;

import com.temporedata.guard.modules.police.photo.entity.CompareTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompareTaskRepository extends JpaRepository<CompareTaskEntity, String> {
    List<CompareTaskEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
    List<CompareTaskEntity> findByTenantIdAndStatusOrderByCreateTimeDesc(String tenantId, String status);
}
