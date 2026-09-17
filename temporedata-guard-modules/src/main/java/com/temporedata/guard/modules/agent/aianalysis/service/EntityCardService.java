/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.agent.EntityCardRes
 *  com.temporedata.guard.api.agent.EntityCardRes$Relations
 *  com.temporedata.guard.api.entitygraph.EntityGraphRes
 *  com.temporedata.guard.api.entitygraph.EntityGraphRes$EntityNode
 *  com.temporedata.guard.api.entitymaster.EntityMasterRes
 *  com.temporedata.guard.api.intelligence.IntelligenceRes
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.agent.aianalysis.service;

import com.temporedata.guard.api.agent.EntityCardRes;
import com.temporedata.guard.api.entitygraph.EntityGraphRes;
import com.temporedata.guard.api.entitymaster.EntityMasterRes;
import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.police.entitygraph.service.EntityGraphService;
import com.temporedata.guard.modules.police.entitymaster.service.EntityMasterService;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.security.LoginUser;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EntityCardService {
    private static final Logger log = LoggerFactory.getLogger(EntityCardService.class);
    private final IntelligenceService intelligenceService;
    private final EntityGraphService entityGraphService;
    private final EntityMasterService entityMasterService;
    private static final List<String> CARD_ACTIONS = List.of("viewDetail", "generateReport");

    public EntityCardRes buildByKeyword(String keyword, LoginUser user) {
        List<IntelligenceRes> hits = this.intelligenceService.search(keyword, null, user);
        if (hits.isEmpty()) {
            return null;
        }
        return this.build(hits.get(0).getEntityId(), user);
    }

    public EntityCardRes build(String entityId, LoginUser user) {
        EntityMasterRes master = this.entityMasterService.detail(entityId, user.getDensity());
        EntityCardRes card = new EntityCardRes();
        card.setEntityId(entityId);
        card.setEntityType(master.getEntityType());
        card.setDisplayName(master.getDisplayName());
        card.setPrimaryKey(master.getPrimaryKey());
        card.setActions(CARD_ACTIONS);
        EntityCardRes.Relations rel = new EntityCardRes.Relations();
        try {
            EntityGraphRes graph = this.entityGraphService.getGraph(entityId, 1, null, user.getTenantId());
            if (graph != null && graph.getNodes() != null) {
                block16: for (EntityGraphRes.EntityNode n : graph.getNodes()) {
                    if (n.getId().equals(entityId)) continue;
                    switch (n.getEntityType() == null ? "" : n.getEntityType()) {
                        case "PERSON": {
                            rel.setPersons(rel.getPersons() + 1);
                            continue block16;
                        }
                        case "VEHICLE": {
                            rel.setVehicles(rel.getVehicles() + 1);
                            continue block16;
                        }
                        case "CASE": {
                            rel.setCases(rel.getCases() + 1);
                            continue block16;
                        }
                        case "ITEM": {
                            rel.setItems(rel.getItems() + 1);
                            continue block16;
                        }
                        case "ORG": {
                            rel.setOrgs(rel.getOrgs() + 1);
                            continue block16;
                        }
                    }
                    rel.setPlaces(rel.getPlaces() + 1);
                }
            }
        }
        catch (Exception e) {
            log.debug("EntityCard relation aggregate skipped: {}", (Object)e.getMessage());
        }
        rel.setTotal(rel.getPersons() + rel.getVehicles() + rel.getCases() + rel.getItems() + rel.getOrgs() + rel.getPlaces());
        card.setRelations(rel);
        return card;
    }

    public EntityCardService(IntelligenceService intelligenceService, EntityGraphService entityGraphService, EntityMasterService entityMasterService) {
        this.intelligenceService = intelligenceService;
        this.entityGraphService = entityGraphService;
        this.entityMasterService = entityMasterService;
    }
}

