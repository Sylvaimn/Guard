package com.temporedata.guard.modules.dev.engine.repository;

import com.temporedata.guard.modules.dev.engine.entity.ResourceQuotaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResourceQuotaRepository extends JpaRepository<ResourceQuotaEntity, String> {
}
