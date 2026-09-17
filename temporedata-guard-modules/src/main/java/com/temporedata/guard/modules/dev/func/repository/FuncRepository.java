package com.temporedata.guard.modules.dev.func.repository;

import com.temporedata.guard.modules.dev.func.entity.FuncEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FuncRepository extends JpaRepository<FuncEntity, String> {

    List<FuncEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}