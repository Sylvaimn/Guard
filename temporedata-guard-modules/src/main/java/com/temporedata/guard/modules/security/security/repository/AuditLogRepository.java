/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.temporedata.guard.modules.security.security.repository;

import com.temporedata.guard.modules.security.security.entity.AuditLogEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AuditLogRepository
extends JpaRepository<AuditLogEntity, String>,
JpaSpecificationExecutor<AuditLogEntity> {
    public List<AuditLogEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public Page<AuditLogEntity> findByTenantIdOrderByCreateTimeDesc(String var1, Pageable var2);
}

