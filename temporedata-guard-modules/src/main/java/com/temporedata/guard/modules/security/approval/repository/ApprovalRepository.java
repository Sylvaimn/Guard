/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.approval.repository;
import com.temporedata.guard.modules.security.approval.entity.ApprovalEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRepository
extends JpaRepository<ApprovalEntity, String> {
    public List<ApprovalEntity> findByTenantIdOrderByCreateTimeDesc(String var1);
}

