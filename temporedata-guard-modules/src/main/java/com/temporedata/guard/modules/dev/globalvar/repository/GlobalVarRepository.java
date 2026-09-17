package com.temporedata.guard.modules.dev.globalvar.repository;

import com.temporedata.guard.modules.dev.globalvar.entity.GlobalVarEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GlobalVarRepository extends JpaRepository<GlobalVarEntity, String> {

    List<GlobalVarEntity> findByTenantId(String tenantId);

    Optional<GlobalVarEntity> findByVarKeyAndTenantId(String varKey, String tenantId);
}