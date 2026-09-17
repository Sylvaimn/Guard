package com.temporedata.guard.modules.platform.notify.repository;

import com.temporedata.guard.modules.platform.notify.entity.NotifyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotifyRepository extends JpaRepository<NotifyEntity, String> {

    List<NotifyEntity> findByTenantId(String tenantId);

    List<NotifyEntity> findByChannelTypeAndTenantId(String channelType, String tenantId);

    void deleteByTenantId(String tenantId);
}