/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.alarm.repository;
import com.temporedata.guard.modules.security.alarm.entity.AlarmRecordEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlarmRecordRepository
extends JpaRepository<AlarmRecordEntity, String> {
    public List<AlarmRecordEntity> findByConfigIdOrderBySendTimeDesc(String var1);
}

