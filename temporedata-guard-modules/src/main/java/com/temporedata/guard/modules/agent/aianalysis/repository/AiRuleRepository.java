/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiRuleEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiRuleRepository
extends JpaRepository<AiRuleEntity, String> {
    public List<AiRuleEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<AiRuleEntity> findByTenantIdAndStatus(String var1, String var2);
}

