/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.data.jpa.repository.JpaRepository
 */
package com.temporedata.guard.modules.security.blacklist.repository;
import com.temporedata.guard.modules.security.blacklist.entity.BlacklistEntity;


import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepository
extends JpaRepository<BlacklistEntity, String> {
    public Optional<BlacklistEntity> findFirstByIpAddressAndListTypeAndStatus(String var1, String var2, String var3);

    public List<BlacklistEntity> findByListTypeAndStatus(String var1, String var2);

    public List<BlacklistEntity> findAllByTenantIdOrderByCreateTimeDesc(String var1);

    public List<BlacklistEntity> findByTenantIdAndIpAddress(String var1, String var2);
}

