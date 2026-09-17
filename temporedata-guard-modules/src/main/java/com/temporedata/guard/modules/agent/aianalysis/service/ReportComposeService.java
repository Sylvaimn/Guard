/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.temporedata.guard.api.agent.EntityCardRes
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiReportEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AiReportRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.agent.EntityCardRes;
import com.temporedata.guard.api.base.exceptions.BusinessException;



import com.temporedata.guard.modules.police.track.service.TrackService;
import com.temporedata.guard.security.LoginUser;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportComposeService {
    private static final Logger log = LoggerFactory.getLogger(ReportComposeService.class);
    private final AiReportRepository reportRepository;
    private final EntityCardService entityCardService;
    private final TrackService trackService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public String compose(String entityId, String tenantId, LoginUser user, String taskId) {
        LinkedHashMap<String, Object> report = new LinkedHashMap<String, Object>();
        report.put("reportId", UUID.randomUUID().toString().replace("-", ""));
        report.put("entityId", entityId);
        EntityCardRes card = this.entityCardService.build(entityId, user);
        if (card != null) {
            report.put("basic", Map.of("displayName", card.getDisplayName(), "entityType", card.getEntityType(), "primaryKey", card.getPrimaryKey(), "relations", card.getRelations() == null ? Map.of() : Map.of("cases", card.getRelations().getCases(), "persons", card.getRelations().getPersons(), "vehicles", card.getRelations().getVehicles(), "items", card.getRelations().getItems(), "orgs", card.getRelations().getOrgs())));
        }
        try {
            Map<String, Object> track = this.trackService.summary(entityId, LocalDateTime.now().minusDays(30L), LocalDateTime.now(), tenantId);
            report.put("track", track);
        }
        catch (Exception e) {
            report.put("track", Map.of("note", "\u6682\u65e0\u8f68\u8ff9\u6570\u636e"));
        }
        report.put("generatedBy", user.getUsername());
        report.put("generatedAt", LocalDateTime.now().toString());
        report.put("source", "\u57fa\u4e8e\u8f7b\u821f\u4e91\u5e73\u53f0\u771f\u5b9e\u6570\u636e\u805a\u5408(\u5b9e\u4f53\u4e3b\u6570\u636e/\u56fe\u8c31\u5173\u8054/\u8f68\u8ff9), \u5efa\u8bae\u4eba\u5de5\u590d\u6838");
        AiReportEntity r = new AiReportEntity();
        r.setId(String.valueOf(report.get("reportId")));
        r.setTaskId(taskId);
        r.setEntityId(entityId);
        r.setEntityName(card == null ? entityId : card.getDisplayName());
        try {
            r.setReportContent(this.objectMapper.writeValueAsString(report));
        }
        catch (Exception e) {
            r.setReportContent(String.valueOf(report));
        }
        r.setTenantId(tenantId);
        r.setCreatorId(user.getUserId());
        r.setCreateTime(LocalDateTime.now());
        this.reportRepository.save(r);
        log.info("[REPORT] composed {} for entity {}", (Object)r.getId(), (Object)entityId);
        return r.getId();
    }

    public List<AiReportEntity> list(String tenantId) {
        return this.reportRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    public AiReportEntity detail(String id, String tenantId) {
        AiReportEntity r = this.reportRepository.findById(id).filter(x -> x.getTenantId().equals(tenantId)).orElseThrow(() -> new BusinessException("\u62a5\u544a\u4e0d\u5b58\u5728"));
        return r;
    }

    public ReportComposeService(AiReportRepository reportRepository, EntityCardService entityCardService, TrackService trackService) {
        this.reportRepository = reportRepository;
        this.entityCardService = entityCardService;
        this.trackService = trackService;
    }
}

