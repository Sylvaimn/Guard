package com.temporedata.guard.modules.platform.org.repository;

import com.temporedata.guard.modules.platform.org.entity.OrgEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrgRepository extends JpaRepository<OrgEntity, String> {

    List<OrgEntity> findByParentId(String parentId);

    List<OrgEntity> findByTenantId(String tenantId);

    void deleteByTenantId(String tenantId);
}