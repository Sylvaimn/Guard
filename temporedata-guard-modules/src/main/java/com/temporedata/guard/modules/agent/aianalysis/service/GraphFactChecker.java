/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.entitygraph.EntityGraphRes
 *  com.temporedata.guard.api.entitygraph.EntityGraphRes$EntityNode
 *  com.temporedata.guard.api.intelligence.IntelligenceRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Component
 */
package com.temporedata.guard.modules.agent.aianalysis.service;

import com.temporedata.guard.api.entitygraph.EntityGraphRes;
import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.police.entitygraph.service.EntityGraphService;
import java.lang.invoke.CallSite;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GraphFactChecker {
    private static final Logger log = LoggerFactory.getLogger(GraphFactChecker.class);
    private final EntityGraphService entityGraphService;

    public Map<String, Object> verify(String tenantId, List<String> chain, List<Map<String, Object>> steps) {
        Object results;
        ArrayList<Map<String, String>> pairs = new ArrayList<Map<String, String>>();
        LinkedHashSet<String> seen = new LinkedHashSet<String>();
        for (Map<String, Object> step : steps) {
            results = step.get("results");
            Object hits = results instanceof Map ? ((Map)results).get("hits") : null;
            if (!(hits instanceof List)) continue;
            for (Object h : (List)hits) {
                String name;
                if (!(h instanceof IntelligenceRes) || (name = ((IntelligenceRes)h).getDisplayName()) == null || !seen.add(name)) continue;
                pairs.add(Map.of("name", name, "entityId", ((IntelligenceRes)h).getEntityId()));
            }
        }
        for (Map<String, Object> step : steps) {
            results = step.get("results");
            Object graph = results instanceof Map ? ((Map)results).get("graph") : null;
            List nodes = null;
            if (graph instanceof EntityGraphRes) {
                nodes = ((EntityGraphRes)graph).getNodes();
            } else if (graph instanceof Map && ((Map)graph).get("nodes") instanceof List) {
                nodes = (List)((Map)graph).get("nodes");
            }
            if (nodes == null) continue;
            for (Object n : nodes) {
                EntityGraphRes.EntityNode node;
                if (n instanceof Map) {
                    Object name = ((Map)n).get("name");
                    Object id = ((Map)n).get("id");
                    if (name == null || !seen.add(String.valueOf(name))) continue;
                    pairs.add(Map.of("name", String.valueOf(name), "entityId", String.valueOf(id)));
                    continue;
                }
                if (!(n instanceof EntityGraphRes.EntityNode) || (node = (EntityGraphRes.EntityNode)n).getName() == null || !seen.add(node.getName())) continue;
                pairs.add(Map.of("name", node.getName(), "entityId", node.getId()));
            }
        }
        if (pairs.size() < 2) {
            return Map.of("pairs", pairs.size(), "verified", 0, "total", 0, "confidence", 0.0, "note", "\u547d\u4e2d\u5b9e\u4f53\u4e0d\u8db3\uff0c\u65e0\u6cd5\u8fdb\u884c\u56fe\u8c31\u5173\u7cfb\u6838\u67e5");
        }
        ArrayList<String> verifiedRels = new ArrayList<String>();
        int total = 0;
        for (int i = 0; i < Math.min(pairs.size(), 3); ++i) {
            for (int j = i + 1; j < Math.min(pairs.size(), 3); ++j) {
                ++total;
                Map a = (Map)pairs.get(i);
                Map b = (Map)pairs.get(j);
                if (!this.connected(tenantId, (String)a.get("entityId"), (String)b.get("entityId"))) continue;
                verifiedRels.add((String) a.get("name") + "\u2194" + (String) b.get("name"));
            }
        }
        double confidence = total == 0 ? 0.0 : (double)verifiedRels.size() * 1.0 / (double)total;
        String note = verifiedRels.isEmpty() ? "\u672a\u53d1\u73b0\u56fe\u8c31\u5df2\u6838\u5b9e\u5173\u7cfb\uff0c\u5efa\u8bae\u4eba\u5de5\u590d\u6838" : "\u56fe\u8c31\u5df2\u6838\u5b9e\u5173\u7cfb: " + String.join((CharSequence)"\u3001", verifiedRels);
        return Map.of("pairs", pairs.size(), "verified", verifiedRels.size(), "total", total, "confidence", confidence, "note", note);
    }

    private boolean connected(String tenantId, String entityIdA, String entityIdB) {
        if (entityIdA == null || entityIdB == null || entityIdA.isBlank() || entityIdB.isBlank()) {
            return false;
        }
        try {
            EntityGraphRes graph = this.entityGraphService.getGraph(entityIdA, 1, null, tenantId == null ? "DEFAULT" : tenantId);
            if (graph == null || graph.getNodes() == null) {
                return false;
            }
            return graph.getNodes().stream().anyMatch(n -> entityIdB.equals(n.getId()));
        }
        catch (Exception e) {
            log.debug("Graph connectivity check failed: {}", (Object)e.getMessage());
            return false;
        }
    }

    public GraphFactChecker(EntityGraphService entityGraphService) {
        this.entityGraphService = entityGraphService;
    }
}

