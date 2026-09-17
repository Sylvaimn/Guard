/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.ObjectProvider
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.agent.aianalysis.rag;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;
import com.temporedata.guard.modules.agent.aianalysis.vector.VectorStore;


import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.intelligence.service.DatabaseSearchEngine;
import com.temporedata.guard.modules.security.security.DensityGuard;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class RagService {
    private static final Logger log = LoggerFactory.getLogger(RagService.class);
    private final DatabaseSearchEngine databaseSearchEngine;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final EntityMasterRepository masterRepository;
    private final EntityLabelRepository labelRepository;
    private static final int KEYWORD_K = 5;
    private static final int VECTOR_K = 5;
    private static final int MERGE_LIMIT = 8;

    public List<Map<String, Object>> retrieve(String question, String tenantId, String density) {
        List<String> densities = DensityGuard.allowedDensities(density);
        LinkedHashMap<String, String> sourceById = new LinkedHashMap<String, String>();
        List<com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity> kwHits = this.safeGet(() -> this.databaseSearchEngine.searchByKeyword(tenantId, densities, question, 5));
        kwHits.forEach(e -> sourceById.putIfAbsent(e.getId(), "keyword"));
        VectorStore vectorStore = this.getVectorStore();
        if (vectorStore != null && vectorStore.available()) {
            List<String> vectorIds = this.safeGet(() -> vectorStore.searchEntityIds(question, 5));
            for (String id : vectorIds) {
                sourceById.putIfAbsent(id, "vector");
            }
        }
        List<com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity> entities = sourceById.isEmpty() ? List.of() : this.masterRepository.findByIdIn(sourceById.keySet());
        return entities.stream().filter(e -> e.getTenantId().equals(tenantId)).filter(e -> DensityGuard.canAccess(density, e.getDensity())).limit(8L).map(e -> {
            LinkedHashMap<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("entityId", e.getId());
            item.put("entityType", e.getEntityType());
            item.put("displayName", e.getDisplayName());
            item.put("labels", this.labelRepository.findByEntityId(e.getId()).stream().map(EntityLabelEntity::getLabelName).filter(n -> n != null).collect(Collectors.toList()));
            item.put("source", sourceById.get(e.getId()));
            return item;
        }).collect(Collectors.toList());
    }

    private VectorStore getVectorStore() {
        try {
            return (VectorStore)this.vectorStoreProvider.getIfAvailable();
        }
        catch (Exception e) {
            return null;
        }
    }

    private <T> T safeGet(Supplier<T> s) {
        try {
            return s.get();
        }
        catch (Exception e) {
            log.warn("RAG retrieve partial failure: {}", (Object)e.getMessage());
            return (T)List.of();
        }
    }

    public RagService(DatabaseSearchEngine databaseSearchEngine, ObjectProvider<VectorStore> vectorStoreProvider, EntityMasterRepository masterRepository, EntityLabelRepository labelRepository) {
        this.databaseSearchEngine = databaseSearchEngine;
        this.vectorStoreProvider = vectorStoreProvider;
        this.masterRepository = masterRepository;
        this.labelRepository = labelRepository;
    }
}

