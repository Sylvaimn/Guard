/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AnomalyClueEntity;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnomalyClueRepository
extends JpaRepository<AnomalyClueEntity, String> {
    public List<AnomalyClueEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<AnomalyClueEntity> findByTenantIdAndStatusOrderByCreateTimeDesc(String var1, String var2);

    public Optional<AnomalyClueEntity> findFirstByTenantIdAndMetricTypeAndTargetAndStatus(String var1, String var2, String var3, String var4);
}

