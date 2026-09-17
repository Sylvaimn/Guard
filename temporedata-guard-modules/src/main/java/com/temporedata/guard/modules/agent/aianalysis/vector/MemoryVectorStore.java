/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.annotation.PostConstruct
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.agent.aianalysis.vector;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;


import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="zy.ai.vector", name={"engine"}, havingValue="memory", matchIfMissing=true)
public class MemoryVectorStore
implements VectorStore {
    private static final Logger log = LoggerFactory.getLogger(MemoryVectorStore.class);
    private final Map<String, Map<String, Double>> entityFeatures = new ConcurrentHashMap<String, Map<String, Double>>();
    private final Map<String, String> entityText = new ConcurrentHashMap<String, String>();
    private final EntityMasterRepository masterRepository;
    private final EntityLabelRepository labelRepository;

    public MemoryVectorStore(EntityMasterRepository masterRepository, EntityLabelRepository labelRepository) {
        this.masterRepository = masterRepository;
        this.labelRepository = labelRepository;
    }

    @PostConstruct
    public void init() {
        try {
            for (EntityMasterEntity e : this.masterRepository.findAll()) {
                List<String> codes = this.labelRepository.findByEntityId(e.getId()).stream().map(l -> l.getLabelCode()).collect(Collectors.toList());
                this.upsertEntity(e, codes);
            }
            log.info("MemoryVectorStore initialized with {} entities", (Object)this.entityFeatures.size());
        }
        catch (Exception e) {
            log.warn("MemoryVectorStore init skipped: {}", (Object)e.getMessage());
        }
    }

    @Override
    public String name() {
        return "memory";
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public void upsertEntity(EntityMasterEntity entity, List<String> labelCodes) {
        String text = String.join((CharSequence)" ", entity.getDisplayName() == null ? "" : entity.getDisplayName(), entity.getPrimaryKey() == null ? "" : entity.getPrimaryKey(), String.join((CharSequence)" ", labelCodes == null ? List.of() : labelCodes));
        this.entityText.put(entity.getId(), text);
        this.entityFeatures.put(entity.getId(), this.tokenize(text));
    }

    @Override
    public void deleteEntity(String entityId) {
        this.entityFeatures.remove(entityId);
        this.entityText.remove(entityId);
    }

    @Override
    public List<String> searchEntityIds(String query, int topK) {
        Map<String, Double> q = this.tokenize(query);
        if (q.isEmpty()) {
            return List.of();
        }
        return this.entityFeatures.entrySet().stream().map(e -> Map.entry((String)e.getKey(), this.cosine(q, (Map)e.getValue()))).filter(e -> (Double)e.getValue() > 0.05).sorted((a, b) -> Double.compare((Double)b.getValue(), (Double)a.getValue())).limit(topK).map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private Map<String, Double> tokenize(String text) {
        ConcurrentHashMap<String, Double> features = new ConcurrentHashMap<String, Double>();
        String s = text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", " ");
        for (int i = 0; i < s.length() - 1; ++i) {
            String gram = s.substring(i, i + 2);
            if (gram.isBlank()) continue;
            features.merge(gram, 1.0, Double::sum);
        }
        return features;
    }

    private double cosine(Map<String, Double> a, Map<String, Double> b) {
        double d;
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        Iterator<Double> iterator = a.values().iterator();
        while (iterator.hasNext()) {
            d = iterator.next();
            na += d * d;
        }
        iterator = b.values().iterator();
        while (iterator.hasNext()) {
            d = iterator.next();
            nb += d * d;
        }
        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }
        for (Map.Entry entry : a.entrySet()) {
            Double bv = b.get(entry.getKey());
            if (bv == null) continue;
            dot += (Double)entry.getValue() * bv;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}

