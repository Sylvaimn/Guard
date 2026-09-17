/*
 * Decompiled with CFR 0.152.
 */
package com.temporedata.guard.modules.agent.aianalysis.vector;

import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import java.util.List;

public interface VectorStore {
    public String name();

    public boolean available();

    public void upsertEntity(EntityMasterEntity var1, List<String> var2);

    public void deleteEntity(String var1);

    public List<String> searchEntityIds(String var1, int var2);
}

