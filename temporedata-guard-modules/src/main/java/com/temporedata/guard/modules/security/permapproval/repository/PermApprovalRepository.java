/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.permapproval.repository;
import com.temporedata.guard.modules.security.permapproval.entity.PermApprovalEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermApprovalRepository
extends JpaRepository<PermApprovalEntity, String> {
    public List<PermApprovalEntity> findByApplicantIdOrderByCreateTimeDesc(String var1);

    public List<PermApprovalEntity> findByStatusOrderByCreateTimeDesc(String var1);
}

