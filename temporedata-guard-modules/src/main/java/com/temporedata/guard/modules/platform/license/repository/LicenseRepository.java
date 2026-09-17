package com.temporedata.guard.modules.platform.license.repository;

import com.temporedata.guard.modules.platform.license.entity.LicenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LicenseRepository extends JpaRepository<LicenseEntity, String> {

    Optional<LicenseEntity> findByTenantId(String tenantId);
}