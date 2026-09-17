package com.temporedata.guard.modules.dev.dependency.repository;

import com.temporedata.guard.modules.dev.dependency.entity.DependencyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DependencyRepository extends JpaRepository<DependencyEntity, String> {

    List<DependencyEntity> findByTenantId(String tenantId);

    List<DependencyEntity> findByTypeAndTenantId(String type, String tenantId);
}