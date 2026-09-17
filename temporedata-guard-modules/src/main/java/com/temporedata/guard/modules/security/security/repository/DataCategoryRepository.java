/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.security.repository;

import com.temporedata.guard.modules.security.security.entity.DataCategoryEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataCategoryRepository
extends JpaRepository<DataCategoryEntity, String> {
    public List<DataCategoryEntity> findByTenantIdOrderByCreateTimeDesc(String var1);

    public List<DataCategoryEntity> findByParentIdAndTenantId(String var1, String var2);
}

