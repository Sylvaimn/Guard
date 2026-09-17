package com.temporedata.guard.modules.dev.quality.repository;

import com.temporedata.guard.modules.dev.quality.entity.QualityCheckEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QualityCheckRepository extends JpaRepository<QualityCheckEntity, String> {

    List<QualityCheckEntity> findByRuleIdOrderByCheckTimeDesc(String ruleId);

    List<QualityCheckEntity> findByRuleIdInOrderByCheckTimeDesc(List<String> ruleIds);
}