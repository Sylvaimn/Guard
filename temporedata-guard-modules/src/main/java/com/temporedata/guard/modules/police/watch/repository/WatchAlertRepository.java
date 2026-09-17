package com.temporedata.guard.modules.police.watch.repository;

import com.temporedata.guard.modules.police.watch.entity.WatchAlertEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchAlertRepository extends JpaRepository<WatchAlertEntity, String> {

    List<WatchAlertEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    List<WatchAlertEntity> findByStatusAndTenantIdOrderByCreateTimeDesc(String status, String tenantId);

    long countByStatusAndTenantId(String status, String tenantId);

    long countByResultAndTenantId(String result, String tenantId);
}
