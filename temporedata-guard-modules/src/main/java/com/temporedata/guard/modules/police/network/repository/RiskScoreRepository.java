package com.temporedata.guard.modules.police.network.repository;

import com.temporedata.guard.modules.police.network.entity.RiskScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskScoreRepository extends JpaRepository<RiskScoreEntity, String> {

    Optional<RiskScoreEntity> findFirstByEntityIdOrderByUpdateTimeDesc(String entityId);

    List<RiskScoreEntity> findByTenantIdAndRiskLevelOrderByScoreDesc(String tenantId, String riskLevel);

    List<RiskScoreEntity> findByTenantIdOrderByScoreDesc(String tenantId);
}
