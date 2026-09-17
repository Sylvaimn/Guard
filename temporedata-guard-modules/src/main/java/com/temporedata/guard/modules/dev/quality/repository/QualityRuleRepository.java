package com.temporedata.guard.modules.dev.quality.repository;

import com.temporedata.guard.modules.dev.quality.entity.QualityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityRuleRepository extends JpaRepository<QualityRuleEntity, String> {

    List<QualityRuleEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    List<QualityRuleEntity> findByDatasourceIdAndTableName(String datasourceId, String tableName);

    List<QualityRuleEntity> findByDatasourceId(String datasourceId);

    long countByLastStatus(String lastStatus);
}