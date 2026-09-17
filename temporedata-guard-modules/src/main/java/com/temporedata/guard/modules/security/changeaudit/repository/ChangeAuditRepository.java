/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.JpaSpecificationExecutor
 */
package com.temporedata.guard.modules.security.changeaudit.repository;
import com.temporedata.guard.modules.security.changeaudit.entity.ChangeAuditEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ChangeAuditRepository
extends JpaRepository<ChangeAuditEntity, String>,
JpaSpecificationExecutor<ChangeAuditEntity> {
    public List<ChangeAuditEntity> findByEntityTypeAndEntityIdOrderByCreateTimeDesc(String var1, String var2);
}

