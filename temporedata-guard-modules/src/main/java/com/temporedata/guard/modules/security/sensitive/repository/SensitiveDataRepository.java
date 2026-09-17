/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.sensitive.repository;
import com.temporedata.guard.modules.security.sensitive.entity.SensitiveDataEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveDataRepository
extends JpaRepository<SensitiveDataEntity, String> {
    public List<SensitiveDataEntity> findByDatasourceIdAndTableName(String var1, String var2);
}

