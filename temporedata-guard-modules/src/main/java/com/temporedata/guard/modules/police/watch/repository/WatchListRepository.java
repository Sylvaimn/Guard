package com.temporedata.guard.modules.police.watch.repository;

import com.temporedata.guard.modules.police.watch.entity.WatchListEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchListRepository extends JpaRepository<WatchListEntity, String> {

    List<WatchListEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    List<WatchListEntity> findByStatusAndTenantId(String status, String tenantId);

    List<WatchListEntity> findByEntityId(String entityId);
}
