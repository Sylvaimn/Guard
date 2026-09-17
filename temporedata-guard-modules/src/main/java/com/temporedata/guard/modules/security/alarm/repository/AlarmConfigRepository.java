/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.alarm.repository;
import com.temporedata.guard.modules.security.alarm.entity.AlarmConfigEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmConfigRepository
extends JpaRepository<AlarmConfigEntity, String> {
    public List<AlarmConfigEntity> findByTenantId(String var1);
}

