package com.temporedata.guard.modules.police.entitymaster.service;
import com.temporedata.guard.modules.security.security.DensityGuard;
import com.temporedata.guard.modules.police.intelligence.service.EntityIndexService;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMappingEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMappingRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.entitymaster.EntityLabelRes;
import com.temporedata.guard.api.entitymaster.EntityMappingRes;
import com.temporedata.guard.api.entitymaster.EntityMasterRes;
import com.temporedata.guard.api.entitymaster.EntityRegisterReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 实体归一化服务（警用）：
 * - 注册来源记录时按 primaryKey 强匹配（规则融合），命中即挂到同一 EntityID
 * - 无命中则建候选实体（CANDIDATE），由人工确认
 * - 人工确认：合并候选 / 拆分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EntityMasterService {

    private final EntityMasterRepository masterRepository;
    private final EntityMappingRepository mappingRepository;
    private final EntityLabelRepository labelRepository;
    private final com.temporedata.guard.modules.police.intelligence.service.EntityIndexService entityIndexService;

    /** 归一化注册：规则融合（primaryKey 强匹配） */
    @Transactional
    public EntityMasterRes register(EntityRegisterReq req, String tenantId, String userDensity) {
        if (req.getEntityType() == null || req.getPrimaryKey() == null || req.getPrimaryKey().isBlank()) {
            throw new BusinessException("实体类型与主标识必填");
        }
        String pk = req.getPrimaryKey().trim();
        // 规则强匹配：同类型同主标识 → 归一到同一实体
        EntityMasterEntity existing = masterRepository.findFirstByPrimaryKeyAndEntityType(pk, req.getEntityType())
                .orElse(null);
        if (existing == null) {
            existing = new EntityMasterEntity();
            existing.setId(UUID.randomUUID().toString().replace("-", ""));
            existing.setEntityType(req.getEntityType());
            existing.setPrimaryKey(pk);
            existing.setDisplayName(req.getDisplayName());
            existing.setDensity(req.getDensity() == null ? "INTERNAL" : req.getDensity().toUpperCase());
            existing.setStatus("CANDIDATE");
            existing.setTenantId(tenantId);
            existing.setCreateTime(LocalDateTime.now());
            existing = masterRepository.save(existing);
            log.info("Created candidate entity {} ({})", existing.getId(), pk);
        } else {
            existing.setUpdateTime(LocalDateTime.now());
            if (req.getDisplayName() != null && (existing.getDisplayName() == null || existing.getDisplayName().isBlank())) {
                existing.setDisplayName(req.getDisplayName());
            }
            masterRepository.save(existing);
        }
        // 登记来源映射（幂等：同源同记录不重复）
        boolean alreadyMapped = mappingRepository.findByEntityId(existing.getId()).stream()
                .anyMatch(m -> m.getSourceSystem().equals(req.getSourceSystem())
                        && m.getSourceRecordId().equals(req.getSourceRecordId()));
        if (!alreadyMapped && req.getSourceSystem() != null && req.getSourceRecordId() != null) {
            EntityMappingEntity mapping = new EntityMappingEntity();
            mapping.setId(UUID.randomUUID().toString().replace("-", ""));
            mapping.setEntityId(existing.getId());
            mapping.setSourceSystem(req.getSourceSystem());
            mapping.setSourceRecordId(req.getSourceRecordId());
            mapping.setMatchMethod(req.getMatchMethod() == null ? "RULE" : req.getMatchMethod());
            mapping.setCreateTime(LocalDateTime.now());
            mappingRepository.save(mapping);
        }
        entityIndexService.syncAsync(existing.getId());
        return detail(existing.getId(), userDensity);
    }

    /** 人工确认：候选实体正式转主数据 */
    @Transactional
    public EntityMasterRes confirm(String entityId, String userDensity) {
        EntityMasterEntity entity = masterRepository.findById(entityId)
                .orElseThrow(() -> new BusinessException("实体不存在"));
        entity.setStatus("MASTER");
        entity.setUpdateTime(LocalDateTime.now());
        EntityMasterEntity saved = masterRepository.save(entity);
        entityIndexService.syncAsync(saved.getId());
        return detail(saved.getId(), userDensity);
    }

    /** 人工合并：把 candidateId 的所有映射并入 masterId，然后删除候选 */
    @Transactional
    public EntityMasterRes merge(String masterId, String candidateId, String userDensity) {
        if (masterId.equals(candidateId)) {
            throw new BusinessException("不能合并自身");
        }
        EntityMasterEntity master = masterRepository.findById(masterId)
                .orElseThrow(() -> new BusinessException("目标实体不存在"));
        EntityMasterEntity candidate = masterRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException("候选实体不存在"));
        // 迁移映射与标签（批量取回，避免 N+1）
        List<EntityMappingEntity> mappings = mappingRepository.findByEntityIdIn(List.of(masterId, candidateId)).stream()
                .filter(m -> candidateId.equals(m.getEntityId())).collect(Collectors.toList());
        Set<String> masterSourceKeys = mappingRepository.findByEntityIdIn(List.of(masterId)).stream()
                .map(m -> m.getSourceSystem() + "|" + m.getSourceRecordId()).collect(Collectors.toSet());
        for (EntityMappingEntity m : mappings) {
            if (!masterSourceKeys.contains(m.getSourceSystem() + "|" + m.getSourceRecordId())) {
                m.setEntityId(masterId);
                mappingRepository.save(m);
            }
        }
        List<EntityLabelEntity> labels = labelRepository.findByEntityIdIn(List.of(masterId, candidateId)).stream()
                .filter(l -> candidateId.equals(l.getEntityId())).collect(Collectors.toList());
        Set<String> masterLabelCodes = labelRepository.findByEntityIdIn(List.of(masterId)).stream()
                .map(EntityLabelEntity::getLabelCode).collect(Collectors.toSet());
        for (EntityLabelEntity l : labels) {
            if (!masterLabelCodes.contains(l.getLabelCode())) {
                l.setEntityId(masterId);
                labelRepository.save(l);
            }
        }
        masterRepository.delete(candidate);
        master.setUpdateTime(LocalDateTime.now());
        EntityMasterEntity merged = masterRepository.save(master);
        entityIndexService.syncAsync(merged.getId());
        return detail(merged.getId(), userDensity);
    }

    /** 打标签 */
    @Transactional
    public EntityLabelRes addLabel(String entityId, String labelType, String labelCode, String labelName,
                                   String source, Double confidence, LocalDateTime expireTime, String tenantId) {
        EntityLabelEntity label = new EntityLabelEntity();
        label.setId(UUID.randomUUID().toString().replace("-", ""));
        label.setEntityId(entityId);
        label.setLabelType(labelType);
        label.setLabelCode(labelCode);
        label.setLabelName(labelName);
        label.setSource(source);
        label.setConfidence(confidence);
        label.setExpireTime(expireTime);
        label.setTenantId(tenantId);
        label.setCreateTime(LocalDateTime.now());
        labelRepository.save(label);
        entityIndexService.syncAsync(entityId);
        return toLabelRes(label);
    }

    public List<EntityMasterRes> list(String entityType, String status, String tenantId, String userDensity) {
        List<EntityMasterEntity> entities;
        if ("CANDIDATE".equals(status)) {
            entities = masterRepository.findByStatusAndTenantIdOrderByCreateTimeDesc("CANDIDATE", tenantId);
        } else if (entityType != null && !entityType.isBlank()) {
            entities = masterRepository.findByEntityTypeAndTenantIdOrderByCreateTimeDesc(entityType, tenantId);
        } else {
            entities = masterRepository.findAllByTenantIdOrderByCreateTimeDesc(tenantId);
        }
        return entities.stream().map(e -> detail(e.getId(), userDensity)).collect(Collectors.toList());
    }

    /** 删除实体（级联映射与标签） */
    @Transactional
    public void delete(String entityId) {
        mappingRepository.findByEntityId(entityId).forEach(mappingRepository::delete);
        labelRepository.findByEntityId(entityId).forEach(labelRepository::delete);
        masterRepository.deleteById(entityId);
        entityIndexService.deleteAsync(entityId);
    }

    public EntityMasterRes detail(String entityId) {
        return detail(entityId, null);
    }

    /** 明细: 用户密级 >= 数据密级时返回明文主标识, 否则脱敏(与情报检索同规则) */
    public EntityMasterRes detail(String entityId, String userDensity) {
        EntityMasterEntity e = masterRepository.findById(entityId)
                .orElseThrow(() -> new BusinessException("实体不存在"));
        EntityMasterRes res = new EntityMasterRes();
        res.setId(e.getId());
        res.setEntityType(e.getEntityType());
        boolean canView = com.temporedata.guard.modules.security.security.DensityGuard
                .canAccess(userDensity, e.getDensity());
        res.setPrimaryKey(canView ? e.getPrimaryKey()
                : com.temporedata.guard.common.util.SensitiveUtil.maskKey(e.getPrimaryKey()));
        res.setDisplayName(e.getDisplayName());
        res.setDensity(e.getDensity());
        res.setStatus(e.getStatus());
        res.setConfidence(e.getConfidence());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setMappings(mappingRepository.findByEntityId(entityId).stream().map(this::toMappingRes).collect(Collectors.toList()));
        res.setLabels(labelRepository.findByEntityId(entityId).stream().map(this::toLabelRes).collect(Collectors.toList()));
        return res;
    }

    private EntityMappingRes toMappingRes(EntityMappingEntity m) {
        EntityMappingRes res = new EntityMappingRes();
        res.setId(m.getId());
        res.setEntityId(m.getEntityId());
        res.setSourceSystem(m.getSourceSystem());
        res.setSourceRecordId(m.getSourceRecordId());
        res.setConfidence(m.getConfidence());
        res.setMatchMethod(m.getMatchMethod());
        res.setCreateTime(m.getCreateTime() != null ? m.getCreateTime().toString() : null);
        return res;
    }

    private EntityLabelRes toLabelRes(EntityLabelEntity l) {
        EntityLabelRes res = new EntityLabelRes();
        res.setId(l.getId());
        res.setEntityId(l.getEntityId());
        res.setLabelType(l.getLabelType());
        res.setLabelCode(l.getLabelCode());
        res.setLabelName(l.getLabelName());
        res.setSource(l.getSource());
        res.setConfidence(l.getConfidence());
        res.setExpireTime(l.getExpireTime() != null ? l.getExpireTime().toString() : null);
        return res;
    }
}
