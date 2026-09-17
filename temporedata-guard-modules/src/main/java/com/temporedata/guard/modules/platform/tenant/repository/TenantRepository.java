package com.temporedata.guard.modules.platform.tenant.repository;

import com.temporedata.guard.modules.platform.tenant.entity.TenantEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
}