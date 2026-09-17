/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiToolPluginEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiToolPluginRepository
extends JpaRepository<AiToolPluginEntity, String> {
    public List<AiToolPluginEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<AiToolPluginEntity> findByTenantIdAndStatus(String var1, String var2);
}

