package com.temporedata.guard.modules.agent.agent.repository;

import com.temporedata.guard.modules.agent.agent.entity.ModelPolicyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModelPolicyRepository extends JpaRepository<ModelPolicyEntity, String> {

    Optional<ModelPolicyEntity> findTopByOrderByUpdateTimeDesc();
}
