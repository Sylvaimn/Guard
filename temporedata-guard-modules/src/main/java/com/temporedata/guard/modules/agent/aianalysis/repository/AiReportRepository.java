/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiReportEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiReportRepository
extends JpaRepository<AiReportEntity, String> {
    public List<AiReportEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<AiReportEntity> findByEntityIdOrderByCreateTimeDesc(String var1);
}

