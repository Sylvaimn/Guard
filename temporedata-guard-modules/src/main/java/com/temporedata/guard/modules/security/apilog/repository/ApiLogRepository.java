/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.jpa.repository.JpaRepository
 *  org.springframework.data.jpa.repository.Modifying
 *  org.springframework.data.jpa.repository.Query
 *  org.springframework.data.repository.query.Param
 */
package com.temporedata.guard.modules.security.apilog.repository;
import com.temporedata.guard.modules.security.apilog.entity.ApiLogEntity;


import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ApiLogRepository
extends JpaRepository<ApiLogEntity, String> {
    public Page<ApiLogEntity> findByTenantIdOrderByCreateTimeDesc(String var1, Pageable var2);

    public List<ApiLogEntity> findByCreateTimeAfter(LocalDateTime var1);

    @Modifying
    @Query(value="delete from ApiLogEntity e where e.createTime < :threshold")
    public int deleteByCreateTimeBefore(@Param(value="threshold") LocalDateTime var1);
}

