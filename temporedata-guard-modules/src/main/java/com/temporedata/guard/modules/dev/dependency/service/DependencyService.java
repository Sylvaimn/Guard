package com.temporedata.guard.modules.dev.dependency.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.dev.dependency.entity.DependencyEntity;
import com.temporedata.guard.modules.dev.dependency.repository.DependencyRepository;
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
public class DependencyService {

    private final DependencyRepository dependencyRepository;

    public List<DependencyEntity> listByTenant(String tenantId) {
        return dependencyRepository.findByTenantId(tenantId);
    }

    public List<DependencyEntity> listByType(String type, String tenantId) {
        return dependencyRepository.findByTypeAndTenantId(type, tenantId);
    }

    @Transactional
    public DependencyEntity create(String name, String description, String type, String config, String tenantId) {
        DependencyEntity entity = new DependencyEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setName(name);
        entity.setDescription(description);
        entity.setType(type);
        entity.setConfig(config);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return dependencyRepository.save(entity);
    }

    @Transactional
    public DependencyEntity update(String id, String name, String description, String type, String config) {
        DependencyEntity entity = dependencyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("依赖合集不存在"));
        entity.setName(name);
        entity.setDescription(description);
        entity.setType(type);
        entity.setConfig(config);
        entity.setUpdateTime(LocalDateTime.now());
        return dependencyRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        dependencyRepository.deleteById(id);
    }
}