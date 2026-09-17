package com.temporedata.guard.modules.dev.tag.service;
import com.temporedata.guard.modules.security.changeaudit.service.ChangeAuditService;
import com.temporedata.guard.modules.dev.tag.entity.ClassificationEntity;
import com.temporedata.guard.modules.dev.tag.repository.ClassificationRepository;
import com.temporedata.guard.modules.dev.tag.entity.TagBindingEntity;
import com.temporedata.guard.modules.dev.tag.repository.TagBindingRepository;
import com.temporedata.guard.modules.dev.tag.entity.TagEntity;
import com.temporedata.guard.modules.dev.tag.repository.TagRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.tag.TagBindingReq;
import com.temporedata.guard.api.tag.TagBindingRes;
import com.temporedata.guard.api.tag.TagReq;
import com.temporedata.guard.api.tag.TagRes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data tag service: CRUD, classification and asset binding with mutual-exclusion / derived validation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;
    private final TagBindingRepository tagBindingRepository;
    private final ClassificationRepository classificationRepository;
    private final ChangeAuditService changeAuditService;

    // ==================== Tag CRUD ====================

    public List<TagRes> listAll(String tenantId) {
        return tagRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public TagRes create(TagReq req, String tenantId) {
        TagEntity entity = new TagEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setDescription(req.getDescription());
        entity.setColor(req.getColor());
        entity.setClassificationId(req.getClassificationId());
        entity.setIsMutuallyExclusive(req.getIsMutuallyExclusive() != null && req.getIsMutuallyExclusive());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : "APPROVED");
        entity.setDerivedFromTagId(req.getDerivedFromTagId());
        entity.setDefinition(req.getDefinition());
        entity.setSynonyms(req.getSynonyms());
        entity.setIsTerm(Boolean.TRUE.equals(req.getIsTerm()));
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        TagEntity saved = tagRepository.save(entity);
        changeAuditService.record("TAG", saved.getId(), saved.getName(), "CREATE",
                null, null, "name=" + saved.getName() + ",status=" + saved.getStatus(), tenantId);
        return toRes(saved);
    }

    @Transactional
    public TagRes update(String id, TagReq req) {
        TagEntity entity = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签不存在"));
        String before = "name=" + entity.getName() + ",status=" + entity.getStatus();
        entity.setName(req.getName());
        entity.setCode(req.getCode());
        entity.setDescription(req.getDescription());
        entity.setColor(req.getColor());
        entity.setClassificationId(req.getClassificationId());
        entity.setIsMutuallyExclusive(req.getIsMutuallyExclusive() != null && req.getIsMutuallyExclusive());
        if (req.getStatus() != null && !req.getStatus().isEmpty()) {
            entity.setStatus(req.getStatus());
        }
        entity.setDerivedFromTagId(req.getDerivedFromTagId());
        entity.setDefinition(req.getDefinition());
        entity.setSynonyms(req.getSynonyms());
        entity.setIsTerm(Boolean.TRUE.equals(req.getIsTerm()));
        entity.setUpdateTime(LocalDateTime.now());
        TagEntity saved = tagRepository.save(entity);
        changeAuditService.record("TAG", saved.getId(), saved.getName(), "UPDATE",
                null, before, "name=" + saved.getName() + ",status=" + saved.getStatus(), saved.getTenantId());
        return toRes(saved);
    }

    @Transactional
    public void delete(String id) {
        TagEntity entity = tagRepository.findById(id).orElse(null);
        tagBindingRepository.deleteByTagId(id);
        tagRepository.deleteById(id);
        if (entity != null) {
            changeAuditService.record("TAG", id, entity.getName(), "DELETE",
                    null, "name=" + entity.getName(), null, entity.getTenantId());
        }
    }

    // ==================== Approval workflow (terms) ====================

    @Transactional
    public TagRes submit(String id) {
        return changeStatus(id, "IN_REVIEW", "DRAFT");
    }

    @Transactional
    public TagRes approve(String id) {
        return changeStatus(id, "APPROVED", "IN_REVIEW");
    }

    @Transactional
    public TagRes reject(String id) {
        return changeStatus(id, "REJECTED", "IN_REVIEW");
    }

    private TagRes changeStatus(String id, String target, String expected) {
        TagEntity entity = tagRepository.findById(id)
                .orElseThrow(() -> new BusinessException("标签/术语不存在"));
        if (expected != null && !expected.equals(entity.getStatus())) {
            throw new BusinessException("当前状态为 " + entity.getStatus() + "，无法执行该操作");
        }
        String before = entity.getStatus();
        entity.setStatus(target);
        entity.setUpdateTime(LocalDateTime.now());
        TagEntity saved = tagRepository.save(entity);
        changeAuditService.record("TAG", saved.getId(), saved.getName(), target,
                null, "status=" + before, "status=" + target, saved.getTenantId());
        return toRes(saved);
    }

    // ==================== Classification ====================

    public List<ClassificationEntity> listClassifications(String tenantId) {
        return classificationRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    @Transactional
    public ClassificationEntity createClassification(String name, String code, String description, String tenantId) {
        ClassificationEntity entity = new ClassificationEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(name);
        entity.setCode(code);
        entity.setDescription(description);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        ClassificationEntity saved = classificationRepository.save(entity);
        changeAuditService.record("CLASSIFICATION", saved.getId(), saved.getName(), "CREATE",
                null, null, "name=" + saved.getName(), tenantId);
        return saved;
    }

    @Transactional
    public void deleteClassification(String id) {
        ClassificationEntity entity = classificationRepository.findById(id).orElse(null);
        List<TagEntity> tags = tagRepository.findByClassificationId(id);
        for (TagEntity tag : tags) {
            tag.setClassificationId(null);
            tagRepository.save(tag);
        }
        classificationRepository.deleteById(id);
        if (entity != null) {
            changeAuditService.record("CLASSIFICATION", id, entity.getName(), "DELETE",
                    null, "name=" + entity.getName(), null, entity.getTenantId());
        }
    }

    // ==================== Asset Binding ====================

    @Transactional
    public void bind(TagBindingReq req, String tenantId) {
        if (req.getAssetType() == null || req.getAssetId() == null) {
            throw new BusinessException("缺少资产类型或资产ID");
        }
        if (req.getTagIds() == null) {
            req.setTagIds(new ArrayList<>());
        }

        // Expand derived tags: when a parent tag is bound, its derived tags are bound too (fixpoint).
        Set<String> finalTagIds = new LinkedHashSet<>(req.getTagIds());
        List<TagEntity> allTags = tagRepository.findByTenantId(tenantId);
        boolean changed = true;
        while (changed) {
            changed = false;
            for (TagEntity tag : allTags) {
                if (tag.getDerivedFromTagId() != null
                        && !tag.getDerivedFromTagId().isEmpty()
                        && finalTagIds.contains(tag.getDerivedFromTagId())
                        && !finalTagIds.contains(tag.getId())) {
                    finalTagIds.add(tag.getId());
                    changed = true;
                }
            }
        }

        // Mutual-exclusion check: no two mutually-exclusive tags of the same classification on one asset.
        Set<String> boundClassificationIds = new HashSet<>();
        for (String tagId : finalTagIds) {
            TagEntity tag = tagRepository.findById(tagId)
                    .orElseThrow(() -> new BusinessException("标签不存在: " + tagId));
            if (tag.getIsMutuallyExclusive() != null && tag.getIsMutuallyExclusive()
                    && tag.getClassificationId() != null) {
                if (!boundClassificationIds.add(tag.getClassificationId())) {
                    throw new BusinessException("互斥标签冲突: 同一分类下只能选择一个标签");
                }
            }
        }

        // Remove previous bindings, then re-bind
        tagBindingRepository.deleteByAssetTypeAndAssetId(req.getAssetType(), req.getAssetId());
        for (String tagId : finalTagIds) {
            TagBindingEntity entity = new TagBindingEntity();
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setTagId(tagId);
            entity.setAssetType(req.getAssetType());
            entity.setAssetId(req.getAssetId());
            entity.setAssetName(req.getAssetName());
            entity.setTenantId(tenantId);
            entity.setCreateTime(LocalDateTime.now());
            tagBindingRepository.save(entity);
        }
        changeAuditService.record("TAG_BINDING", req.getAssetId(), req.getAssetName(), "BIND",
                null, null, "tags=" + String.join(",", finalTagIds), tenantId);
    }

    @Transactional
    public void unbind(String tagId, String assetType, String assetId) {
        List<TagBindingEntity> removed = tagBindingRepository.findByTagIdAndAssetTypeAndAssetId(tagId, assetType, assetId);
        tagBindingRepository.deleteByTagIdAndAssetTypeAndAssetId(tagId, assetType, assetId);
        if (!removed.isEmpty()) {
            TagBindingEntity b = removed.get(0);
            changeAuditService.record("TAG_BINDING", assetId, b.getAssetName(), "UNBIND",
                    null, "tag=" + tagId, null, b.getTenantId());
        }
    }

    public List<TagBindingRes> listByAsset(String assetType, String assetId) {
        return tagBindingRepository.findByAssetTypeAndAssetId(assetType, assetId).stream()
                .map(b -> {
                    TagBindingRes res = toBindingRes(b);
                    tagRepository.findById(b.getTagId()).ifPresent(tag -> {
                        res.setTagName(tag.getName());
                        res.setTagColor(tag.getColor());
                    });
                    return res;
                })
                .collect(Collectors.toList());
    }

    public List<TagBindingRes> listAllBindings(String tenantId) {
        List<TagBindingEntity> bindings = tagBindingRepository.findAll().stream()
                .filter(b -> tenantId.equals(b.getTenantId()))
                .collect(Collectors.toList());
        List<TagBindingRes> result = new ArrayList<>();
        for (TagBindingEntity b : bindings) {
            TagBindingRes res = toBindingRes(b);
            tagRepository.findById(b.getTagId()).ifPresent(tag -> {
                res.setTagName(tag.getName());
                res.setTagColor(tag.getColor());
            });
            result.add(res);
        }
        return result;
    }

    private TagBindingRes toBindingRes(TagBindingEntity b) {
        TagBindingRes res = new TagBindingRes();
        res.setTagId(b.getTagId());
        res.setAssetType(b.getAssetType());
        res.setAssetId(b.getAssetId());
        res.setAssetName(b.getAssetName());
        res.setCreateTime(b.getCreateTime() != null ? b.getCreateTime().toString() : null);
        return res;
    }

    private TagRes toRes(TagEntity e) {
        TagRes res = new TagRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setCode(e.getCode());
        res.setDescription(e.getDescription());
        res.setColor(e.getColor());
        res.setClassificationId(e.getClassificationId());
        if (e.getClassificationId() != null) {
            classificationRepository.findById(e.getClassificationId())
                    .ifPresent(c -> res.setClassificationName(c.getName()));
        }
        res.setIsMutuallyExclusive(e.getIsMutuallyExclusive());
        res.setStatus(e.getStatus());
        res.setDerivedFromTagId(e.getDerivedFromTagId());
        res.setDefinition(e.getDefinition());
        res.setSynonyms(e.getSynonyms());
        res.setIsTerm(Boolean.TRUE.equals(e.getIsTerm()));
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }
}