package com.temporedata.guard.modules.platform.settings.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.platform.settings.entity.SettingsEntity;
import com.temporedata.guard.modules.platform.settings.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettingsService {

    private final SettingsRepository settingsRepository;

    public Map<String, String> getByGroup(String groupName) {
        Map<String, String> map = new LinkedHashMap<>();
        for (SettingsEntity e : settingsRepository.findByGroupName(groupName)) {
            map.put(e.getConfigKey(), e.getConfigValue());
        }
        return map;
    }

    public String getValue(String key) {
        return settingsRepository.findByConfigKey(key)
                .map(SettingsEntity::getConfigValue)
                .orElse(null);
    }

    @Transactional
    public void saveSettings(String groupName, Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            SettingsEntity entity = settingsRepository.findByConfigKey(entry.getKey())
                    .orElseGet(() -> {
                        SettingsEntity e = new SettingsEntity();
                        e.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
                        e.setConfigKey(entry.getKey());
                        e.setGroupName(groupName);
                        e.setCreateTime(LocalDateTime.now());
                        return e;
                    });
            entity.setConfigValue(entry.getValue());
            entity.setUpdateTime(LocalDateTime.now());
            settingsRepository.save(entity);
        }
    }

    @Transactional
    public SettingsEntity setValue(String key, String value, String description) {
        SettingsEntity entity = settingsRepository.findByConfigKey(key)
                .orElseGet(() -> {
                    SettingsEntity e = new SettingsEntity();
                    e.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
                    e.setConfigKey(key);
                    e.setCreateTime(LocalDateTime.now());
                    return e;
                });
        entity.setConfigValue(value);
        if (description != null) entity.setDescription(description);
        entity.setUpdateTime(LocalDateTime.now());
        return settingsRepository.save(entity);
    }

    @Transactional
    public void deleteKey(String key) {
        SettingsEntity entity = settingsRepository.findByConfigKey(key)
                .orElseThrow(() -> new BusinessException("配置项不存在"));
        settingsRepository.delete(entity);
    }

    public List<SettingsEntity> listAll() {
        return settingsRepository.findAll();
    }
}