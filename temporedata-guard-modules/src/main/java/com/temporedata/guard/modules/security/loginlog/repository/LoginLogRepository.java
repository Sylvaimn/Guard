/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.loginlog.repository;
import com.temporedata.guard.modules.security.loginlog.entity.LoginLogEntity;


import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginLogRepository
extends JpaRepository<LoginLogEntity, String> {
    public Page<LoginLogEntity> findByTenantIdOrderByCreateTimeDesc(String var1, Pageable var2);

    public List<LoginLogEntity> findByUserIdOrderByCreateTimeDesc(String var1);
}

