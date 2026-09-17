package com.temporedata.guard.modules.platform.notify.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.platform.notify.entity.NotifyEntity;
import com.temporedata.guard.modules.platform.notify.repository.NotifyRepository;
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
public class NotifyService {

    private final NotifyRepository notifyRepository;

    public List<NotifyEntity> listByTenant(String tenantId) {
        return notifyRepository.findByTenantId(tenantId);
    }

    @Transactional
    public NotifyEntity create(String channelType, String name, String config, String tenantId) {
        NotifyEntity entity = new NotifyEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setChannelType(channelType);
        entity.setName(name);
        entity.setConfig(config);
        entity.setEnabled(0);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return notifyRepository.save(entity);
    }

    @Transactional
    public NotifyEntity update(String id, String channelType, String name, String config) {
        NotifyEntity entity = notifyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("通知配置不存在"));
        entity.setChannelType(channelType);
        entity.setName(name);
        entity.setConfig(config);
        entity.setUpdateTime(LocalDateTime.now());
        return notifyRepository.save(entity);
    }

    @Transactional
    public void toggleEnabled(String id, boolean enabled) {
        NotifyEntity entity = notifyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("通知配置不存在"));
        entity.setEnabled(enabled ? 1 : 0);
        entity.setUpdateTime(LocalDateTime.now());
        notifyRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        notifyRepository.deleteById(id);
    }
}