package com.temporedata.guard.modules.police.watch.service;
import com.temporedata.guard.modules.police.watch.entity.WatchAlertEntity;
import com.temporedata.guard.modules.police.watch.repository.WatchAlertRepository;
import com.temporedata.guard.modules.police.watch.entity.WatchListEntity;
import com.temporedata.guard.modules.police.watch.repository.WatchListRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.watch.WatchAlertRes;
import com.temporedata.guard.api.watch.WatchListRes;

import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 警用布控预警服务：
 * 名单管理（分组/级别/有效期/密级）→ 命中生成预警 → 签收/处置/反馈 → 战果沉淀。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WatchService {

    private final WatchListRepository listRepository;
    private final WatchAlertRepository alertRepository;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    // ==================== 布控名单 ====================

    @Transactional
    public WatchListRes createList(String entityId, String entityType, String targetName, String listGroup,
                                   Integer level, String reason, String density,
                                   LocalDateTime effectiveFrom, LocalDateTime effectiveTo, String tenantId) {
        if (entityId == null || entityId.isBlank()) {
            throw new BusinessException("布控对象（实体ID）必填");
        }
        WatchListEntity entity = new WatchListEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setEntityId(entityId);
        entity.setEntityType(entityType == null ? "PERSON" : entityType);
        entity.setTargetName(targetName);
        entity.setListGroup(listGroup == null ? "市级" : listGroup);
        entity.setLevel(level == null ? 2 : level);
        entity.setReason(reason);
        entity.setDensity(density == null ? "SECRET" : density.toUpperCase());
        entity.setEffectiveFrom(effectiveFrom);
        entity.setEffectiveTo(effectiveTo);
        entity.setStatus("ACTIVE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toListRes(listRepository.save(entity));
    }

    @Transactional
    public WatchListRes updateListStatus(String id, String status) {
        WatchListEntity entity = listRepository.findById(id)
                .orElseThrow(() -> new BusinessException("名单不存在"));
        entity.setStatus(status);
        entity.setUpdateTime(LocalDateTime.now());
        return toListRes(listRepository.save(entity));
    }

    public List<WatchListRes> listLists(String status, String tenantId) {
        List<WatchListEntity> lists = status != null && !status.isBlank()
                ? listRepository.findByStatusAndTenantId(status, tenantId)
                : listRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
        return lists.stream().map(this::toListRes).collect(Collectors.toList());
    }

    /** 命中检查：实体是否在有效布控名单中 */
    public List<WatchListEntity> matchLists(String entityId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        return listRepository.findByEntityId(entityId).stream()
                .filter(l -> l.getTenantId().equals(tenantId))
                .filter(l -> "ACTIVE".equals(l.getStatus()))
                .filter(l -> l.getEffectiveFrom() == null || !now.isBefore(l.getEffectiveFrom()))
                .filter(l -> l.getEffectiveTo() == null || !now.isAfter(l.getEffectiveTo()))
                .collect(Collectors.toList());
    }

    // ==================== 预警闭环 ====================

    /** 命中生成预警（接入事件调用：卡口过车、核验等） */
    @Transactional
    public WatchAlertRes fireAlert(String entityId, String targetName, String hitScene, String hitSource,
                                   String tenantId) {
        io.micrometer.core.instrument.Counter.builder("watch_hit_total").description("布控预警触发量")
                .tag("scene", String.valueOf(hitScene)).register(meterRegistry).increment();
        List<WatchListEntity> matches = matchLists(entityId, tenantId);
        if (matches.isEmpty()) {
            return null;
        }
        WatchListEntity list = matches.get(0);
        WatchAlertEntity alert = new WatchAlertEntity();
        alert.setId(UUID.randomUUID().toString().replace("-", ""));
        alert.setWatchListId(list.getId());
        alert.setEntityId(entityId);
        alert.setTargetName(targetName != null ? targetName : list.getTargetName());
        alert.setHitScene(hitScene);
        alert.setHitSource(hitSource);
        alert.setLevel(list.getLevel());
        alert.setDensity(list.getDensity());
        alert.setStatus("PENDING");
        alert.setTenantId(tenantId);
        alert.setCreateTime(LocalDateTime.now());
        WatchAlertEntity saved = alertRepository.save(alert);
        log.info("ALERT fired: entity={} scene={} level={}", entityId, hitScene, list.getLevel());
        return toAlertRes(saved);
    }

    /** 签收 */
    @Transactional
    public WatchAlertRes assign(String id, String assignee, String tenantId) {
        WatchAlertEntity alert = getAlert(id, tenantId);
        if (!"PENDING".equals(alert.getStatus())) {
            throw new BusinessException("该预警已签收");
        }
        alert.setStatus("PROCESSING");
        alert.setAssignee(assignee);
        alert.setAssignTime(LocalDateTime.now());
        return toAlertRes(alertRepository.save(alert));
    }

    /** 处置反馈（结果：CAPTURED/EXCLUDED/PENDING + 战果） */
    @Transactional
    public WatchAlertRes process(String id, String feedback, String result, String merit, String tenantId) {
        WatchAlertEntity alert = getAlert(id, tenantId);
        alert.setStatus("CLOSED");
        alert.setFeedback(feedback);
        alert.setResult(result);
        alert.setMerit(merit);
        alert.setCloseTime(LocalDateTime.now());
        return toAlertRes(alertRepository.save(alert));
    }

    public List<WatchAlertRes> listAlerts(String status, String tenantId) {
        List<WatchAlertEntity> alerts = status != null && !status.isBlank()
                ? alertRepository.findByStatusAndTenantIdOrderByCreateTimeDesc(status, tenantId)
                : alertRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
        return alerts.stream().map(this::toAlertRes).collect(Collectors.toList());
    }

    /** 战果统计 */
    public Map<String, Object> stats(String tenantId) {
        return Map.of(
                "pending", alertRepository.countByStatusAndTenantId("PENDING", tenantId),
                "processing", alertRepository.countByStatusAndTenantId("PROCESSING", tenantId),
                "closed", alertRepository.countByStatusAndTenantId("CLOSED", tenantId),
                "captured", alertRepository.countByResultAndTenantId("CAPTURED", tenantId),
                "excluded", alertRepository.countByResultAndTenantId("EXCLUDED", tenantId)
        );
    }

    private WatchAlertEntity getAlert(String id, String tenantId) {
        WatchAlertEntity alert = alertRepository.findById(id)
                .orElseThrow(() -> new BusinessException("预警不存在"));
        if (!alert.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权访问该预警");
        }
        return alert;
    }

    private WatchListRes toListRes(WatchListEntity e) {
        WatchListRes res = new WatchListRes();
        res.setId(e.getId());
        res.setEntityId(e.getEntityId());
        res.setEntityType(e.getEntityType());
        res.setTargetName(e.getTargetName());
        res.setListGroup(e.getListGroup());
        res.setLevel(e.getLevel());
        res.setReason(e.getReason());
        res.setDensity(e.getDensity());
        res.setStatus(e.getStatus());
        res.setEffectiveFrom(e.getEffectiveFrom() != null ? e.getEffectiveFrom().toString() : null);
        res.setEffectiveTo(e.getEffectiveTo() != null ? e.getEffectiveTo().toString() : null);
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private WatchAlertRes toAlertRes(WatchAlertEntity e) {
        WatchAlertRes res = new WatchAlertRes();
        res.setId(e.getId());
        res.setWatchListId(e.getWatchListId());
        res.setEntityId(e.getEntityId());
        res.setTargetName(e.getTargetName());
        res.setHitScene(e.getHitScene());
        res.setHitSource(e.getHitSource());
        res.setLevel(e.getLevel());
        res.setDensity(e.getDensity());
        res.setStatus(e.getStatus());
        res.setAssignee(e.getAssignee());
        res.setFeedback(e.getFeedback());
        res.setResult(e.getResult());
        res.setMerit(e.getMerit());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setAssignTime(e.getAssignTime() != null ? e.getAssignTime().toString() : null);
        res.setCloseTime(e.getCloseTime() != null ? e.getCloseTime().toString() : null);
        return res;
    }
}
