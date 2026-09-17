package com.temporedata.guard.modules.dev.indicator.repository;

import com.temporedata.guard.modules.dev.indicator.entity.IndicatorRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndicatorRunRepository extends JpaRepository<IndicatorRunEntity, String> {

    List<IndicatorRunEntity> findByIndicatorIdOrderByExecuteTimeDesc(String indicatorId);

    Optional<IndicatorRunEntity> findTopByIndicatorIdOrderByExecuteTimeDesc(String indicatorId);

    long countByTenantId(String tenantId);

    long countByTenantIdAndStatus(String tenantId, String status);

    void deleteByIndicatorId(String indicatorId);
}
