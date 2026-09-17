package com.temporedata.guard.modules.agent.agent.repository;

import com.temporedata.guard.modules.agent.agent.entity.AgentConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AgentConfigRepository extends JpaRepository<AgentConfigEntity, String> {

    Optional<AgentConfigEntity> findByTenantId(String tenantId);
}