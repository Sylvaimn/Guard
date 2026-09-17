package com.temporedata.guard.modules.platform.secret.repository;

import com.temporedata.guard.modules.platform.secret.entity.SecretEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SecretRepository extends JpaRepository<SecretEntity, String> {

    List<SecretEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    Optional<SecretEntity> findByTenantIdAndKey(String tenantId, String key);
}