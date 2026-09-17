/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.mydata.repository;
import com.temporedata.guard.modules.security.mydata.entity.MyDataEntity;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MyDataRepository
extends JpaRepository<MyDataEntity, String> {
    public List<MyDataEntity> findByUserIdAndResourceType(String var1, String var2);

    public List<MyDataEntity> findByUserId(String var1);

    public Optional<MyDataEntity> findByUserIdAndResourceId(String var1, String var2);
}

