package com.temporedata.guard.modules.dev.real.repository;

import com.temporedata.guard.modules.dev.real.entity.RealJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RealJobRepository extends JpaRepository<RealJobEntity, String> {

    List<RealJobEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}