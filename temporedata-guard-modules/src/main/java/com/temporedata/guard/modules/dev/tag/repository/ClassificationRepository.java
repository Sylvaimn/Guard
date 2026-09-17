package com.temporedata.guard.modules.dev.tag.repository;

import com.temporedata.guard.modules.dev.tag.entity.ClassificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ClassificationRepository extends JpaRepository<ClassificationEntity, String> {

    List<ClassificationEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}