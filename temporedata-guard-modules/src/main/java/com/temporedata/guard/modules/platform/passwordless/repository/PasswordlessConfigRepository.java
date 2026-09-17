package com.temporedata.guard.modules.platform.passwordless.repository;

import com.temporedata.guard.modules.platform.passwordless.entity.PasswordlessConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Passwordless config repository.
 */
public interface PasswordlessConfigRepository extends JpaRepository<PasswordlessConfigEntity, String> {

    List<PasswordlessConfigEntity> findByStatusAndTenantId(String status, String tenantId);

    Optional<PasswordlessConfigEntity> findByLoginTypeAndTenantId(String loginType, String tenantId);
}