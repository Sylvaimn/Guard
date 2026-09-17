/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.behaviorlog.repository;
import com.temporedata.guard.modules.security.behaviorlog.entity.BehaviorLogEntity;


import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BehaviorLogRepository
extends JpaRepository<BehaviorLogEntity, String> {
    public Page<BehaviorLogEntity> findByTenantIdOrderByCreateTimeDesc(String var1, Pageable var2);

    public List<BehaviorLogEntity> findByUserIdOrderByCreateTimeDesc(String var1);
}

