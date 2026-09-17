/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiTaskEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiTaskRepository
extends JpaRepository<AiTaskEntity, String> {
    public List<AiTaskEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<AiTaskEntity> findByTenantIdAndStatusOrderByCreateTimeDesc(String var1, String var2);

    public List<AiTaskEntity> findByStatusOrderByPriorityAscCreateTimeAsc(String var1);
}

