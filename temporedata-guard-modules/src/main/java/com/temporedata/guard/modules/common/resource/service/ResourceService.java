package com.temporedata.guard.modules.common.resource.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.common.resource.entity.ResourceEntity;
import com.temporedata.guard.modules.common.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;

    /** 血缘登记时持久化关联表与来源方向(供血缘重建恢复边) */
    @Transactional
    public void updateLineageRelation(String id, String tableId, String sourceType) {
        ResourceEntity r = get(id);
        if (tableId != null) r.setTableId(tableId);
        if (sourceType != null) r.setSourceType(sourceType);
        resourceRepository.save(r);
    }

    /** 上传文件自动生成资源 + 血缘关联持久化 */
    @Transactional
    public ResourceEntity createFromFile(String id, String name, String type, String path,
                                         Long size, String tenantId, String tableId, String sourceType) {
        ResourceEntity entity = new ResourceEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setType(type);
        entity.setPath(path);
        entity.setSize(size);
        entity.setTenantId(tenantId);
        entity.setTableId(tableId);
        entity.setSourceType(sourceType);
        entity.setLineageable(true);
        entity.setCreateTime(java.time.LocalDateTime.now());
        return resourceRepository.save(entity);
    }

    public ResourceEntity get(String id) {
        return resourceRepository.findById(id)
                .orElseThrow(() -> new com.temporedata.guard.api.base.exceptions.BusinessException("资源不存在"));
    }

    public List<ResourceEntity> listByTenant(String tenantId) {
        return resourceRepository.findByTenantId(tenantId);
    }

    public List<ResourceEntity> listByType(String type, String tenantId) {
        return resourceRepository.findByTypeAndTenantId(type, tenantId);
    }

    @Transactional
    public ResourceEntity create(String name, String type, String path, Long size,
                                  String description, String tenantId) {
        ResourceEntity entity = new ResourceEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setName(name);
        entity.setType(type);
        entity.setPath(path);
        entity.setSize(size);
        entity.setDescription(description);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return resourceRepository.save(entity);
    }

    @Transactional
    public ResourceEntity update(String id, String name, String description) {
        ResourceEntity entity = resourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("资源不存在"));
        entity.setName(name);
        entity.setDescription(description);
        entity.setUpdateTime(LocalDateTime.now());
        return resourceRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        resourceRepository.deleteById(id);
    }
}