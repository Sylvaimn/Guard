package com.temporedata.guard.modules.common.view.repository;

import com.temporedata.guard.modules.common.view.entity.ViewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ViewRepository extends JpaRepository<ViewEntity, String> {

    List<ViewEntity> findByTenantId(String tenantId);

    List<ViewEntity> findByDatasourceId(String datasourceId);

    List<ViewEntity> findByStatus(String status);
}