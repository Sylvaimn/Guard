/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.agent.aianalysis.repository;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiMemoryEntity;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiMemoryRepository
extends JpaRepository<AiMemoryEntity, String> {
    public List<AiMemoryEntity> findByUserIdAndTenantIdAndTypeOrderByAccessTimeDesc(String var1, String var2, String var3);

    public Optional<AiMemoryEntity> findFirstByUserIdAndTenantIdAndKeyOrderByAccessTimeDesc(String var1, String var2, String var3);
}

