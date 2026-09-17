package com.temporedata.guard.modules.common.driver.repository;

import com.temporedata.guard.modules.common.driver.entity.DriverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DriverRepository extends JpaRepository<DriverEntity, String> {

    List<DriverEntity> findByTenantId(String tenantId);

    Optional<DriverEntity> findByDbTypeAndTenantId(String dbType, String tenantId);
}