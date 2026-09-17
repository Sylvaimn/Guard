/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.message.repository;
import com.temporedata.guard.modules.security.message.entity.MessageEntity;


import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository
extends JpaRepository<MessageEntity, String> {
    public List<MessageEntity> findByUserIdAndTenantIdOrderByCreateTimeDesc(String var1, String var2);

    public List<MessageEntity> findByUserIdAndIsRead(String var1, int var2);

    public long countByUserIdAndIsRead(String var1, int var2);

    public void deleteByUserId(String var1);
}

