/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.beans.factory.annotation.Value
 *  org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
 *  org.springframework.jdbc.core.JdbcTemplate
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.agent.aianalysis.vector;


import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix="zy.ai.vector", name={"engine"}, havingValue="pgvector")
public class PgVectorStore
implements VectorStore {
    private static final Logger log = LoggerFactory.getLogger(PgVectorStore.class);
    private static final int DIM = 384;
    private final JdbcTemplate jdbcTemplate;
    private final boolean ready;

    public PgVectorStore(DataSource dataSource, @Value(value="${zy.ai.vector.dim:384}") int dim) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        boolean ok = false;
        try {
            this.jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            this.jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS temporedata_guard_entity_vector (  entity_id VARCHAR(32) PRIMARY KEY,  tenant_id VARCHAR(32) NOT NULL,  density VARCHAR(16) NOT NULL,  text TEXT NOT NULL,  embedding vector(384))");
            ok = true;
        }
        catch (Exception e) {
            log.warn("pgvector unavailable (fallback to keyword search): {}", (Object)e.getMessage());
        }
        this.ready = ok;
        if (ok) {
            log.info("PgVectorStore ready (dim={})", (Object)384);
        }
    }

    @Override
    public String name() {
        return "pgvector";
    }

    @Override
    public boolean available() {
        return this.ready;
    }

    @Override
    public void upsertEntity(EntityMasterEntity entity, List<String> labelCodes) {
        if (!this.ready) {
            return;
        }
        try {
            String text = String.join((CharSequence)" ", this.nullToEmpty(entity.getDisplayName()), this.nullToEmpty(entity.getPrimaryKey()), labelCodes == null ? "" : String.join((CharSequence)" ", labelCodes));
            float[] emb = this.embed(text);
            this.jdbcTemplate.update("INSERT INTO temporedata_guard_entity_vector (entity_id, tenant_id, density, text, embedding) VALUES (?,?,?,?,?::vector) ON CONFLICT (entity_id) DO UPDATE SET text = EXCLUDED.text, embedding = EXCLUDED.embedding", new Object[]{entity.getId(), entity.getTenantId(), entity.getDensity(), text, this.toVectorLiteral(emb)});
        }
        catch (Exception e) {
            log.debug("pgvector upsert failed: {}", (Object)e.getMessage());
        }
    }

    @Override
    public void deleteEntity(String entityId) {
        if (!this.ready) {
            return;
        }
        try {
            this.jdbcTemplate.update("DELETE FROM temporedata_guard_entity_vector WHERE entity_id = ?", new Object[]{entityId});
        }
        catch (Exception e) {
            log.debug("pgvector delete failed: {}", (Object)e.getMessage());
        }
    }

    @Override
    public List<String> searchEntityIds(String query, int topK) {
        if (!this.ready) {
            return List.of();
        }
        try {
            float[] q = this.embed(query);
            return this.jdbcTemplate.queryForList("SELECT entity_id FROM temporedata_guard_entity_vector ORDER BY embedding <=> ?::vector LIMIT ?", String.class, new Object[]{this.toVectorLiteral(q), topK});
        }
        catch (Exception e) {
            log.debug("pgvector search failed: {}", (Object)e.getMessage());
            return List.of();
        }
    }

    private float[] embed(String text) {
        float[] vec = new float[384];
        String s = text == null ? "" : text.toLowerCase().replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", " ");
        for (int i = 0; i < s.length() - 1; ++i) {
            Object gram = s.substring(i, i + 2);
            if (((String)gram).isBlank()) continue;
            int n = Math.floorMod(((String)gram).hashCode(), 384);
            vec[n] = vec[n] + 1.0f;
        }
        float norm = 0.0f;
        for (float v : vec) {
            norm += v * v;
        }
        if ((norm = (float)Math.sqrt(norm)) > 0.0f) {
            int i = 0;
            while (i < 384) {
                int n = i++;
                vec[n] = vec[n] / norm;
            }
        }
        return vec;
    }

    private String toVectorLiteral(float[] vec) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vec.length; ++i) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vec[i]);
        }
        return sb.append("]").toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

