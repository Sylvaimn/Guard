package com.temporedata.guard.modules.dev.indicator.repository;

import com.temporedata.guard.modules.dev.indicator.entity.IndicatorEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IndicatorRepository extends JpaRepository<IndicatorEntity, String> {

    List<IndicatorEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    Page<IndicatorEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId, Pageable pageable);

    Optional<IndicatorEntity> findByCode(String code);
}