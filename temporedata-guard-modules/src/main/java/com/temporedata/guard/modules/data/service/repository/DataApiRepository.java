package com.temporedata.guard.modules.data.service.repository;

import com.temporedata.guard.modules.data.service.entity.DataApiEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DataApiRepository extends JpaRepository<DataApiEntity, String> {

    List<DataApiEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    Optional<DataApiEntity> findByPath(String path);

    Optional<DataApiEntity> findByPathAndApiKey(String path, String apiKey);
}