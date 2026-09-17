/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.security.repository;

import com.temporedata.guard.modules.security.security.entity.MaskRuleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaskRuleRepository
extends JpaRepository<MaskRuleEntity, String> {
    public List<MaskRuleEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<MaskRuleEntity> findByDatasourceIdAndTableNameAndColumnNameAndStatus(String var1, String var2, String var3, Integer var4);
}

