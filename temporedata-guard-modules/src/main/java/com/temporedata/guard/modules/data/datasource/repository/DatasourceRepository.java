package com.temporedata.guard.modules.data.datasource.repository;

import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasourceRepository extends JpaRepository<DatasourceEntity, String> {

    List<DatasourceEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}