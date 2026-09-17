/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.security.repository;

import com.temporedata.guard.modules.security.security.entity.DataLevelEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataLevelRepository
extends JpaRepository<DataLevelEntity, String> {
    public List<DataLevelEntity> findByTenantIdOrderBySortOrderAsc(String var1);

    public boolean existsByCodeAndTenantId(String var1, String var2);
}

