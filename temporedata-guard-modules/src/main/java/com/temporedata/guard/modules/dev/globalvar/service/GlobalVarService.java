package com.temporedata.guard.modules.dev.globalvar.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.dev.globalvar.entity.GlobalVarEntity;
import com.temporedata.guard.modules.dev.globalvar.repository.GlobalVarRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GlobalVarService {

    private final GlobalVarRepository globalVarRepository;

    public List<GlobalVarEntity> listByTenant(String tenantId) {
        return globalVarRepository.findByTenantId(tenantId);
    }

    public Map<String, String> asMap(String tenantId) {
        return globalVarRepository.findByTenantId(tenantId).stream()
                .collect(Collectors.toMap(GlobalVarEntity::getVarKey, GlobalVarEntity::getVarValue));
    }

    @Transactional
    public GlobalVarEntity create(String varKey, String varValue, String description, String tenantId) {
        if (globalVarRepository.findByVarKeyAndTenantId(varKey, tenantId).isPresent()) {
            throw new BusinessException("变量名已存在");
        }
        GlobalVarEntity entity = new GlobalVarEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setVarKey(varKey);
        entity.setVarValue(varValue);
        entity.setDescription(description);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return globalVarRepository.save(entity);
    }

    @Transactional
    public GlobalVarEntity update(String id, String varKey, String varValue, String description) {
        GlobalVarEntity entity = globalVarRepository.findById(id)
                .orElseThrow(() -> new BusinessException("变量不存在"));
        entity.setVarKey(varKey);
        entity.setVarValue(varValue);
        entity.setDescription(description);
        entity.setUpdateTime(LocalDateTime.now());
        return globalVarRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        globalVarRepository.deleteById(id);
    }
}