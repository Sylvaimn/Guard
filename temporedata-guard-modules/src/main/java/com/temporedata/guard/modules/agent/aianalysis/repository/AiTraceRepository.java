/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiTraceEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTraceRepository
extends JpaRepository<AiTraceEntity, String> {
    public List<AiTraceEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<AiTraceEntity> findBySessionIdOrderByCreateTimeAsc(String var1);
}

