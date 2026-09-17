package com.temporedata.guard.modules.platform.preference.service;

import com.temporedata.guard.api.preference.PreferenceReq;
import com.temporedata.guard.api.preference.PreferenceRes;
import com.temporedata.guard.modules.platform.preference.entity.PreferenceEntity;
import com.temporedata.guard.modules.platform.preference.repository.PreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * User preference service: get, save, batch save.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    public List<PreferenceRes> getByUserId(String userId) {
        return preferenceRepository.findByUserId(userId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public PreferenceRes saveOrUpdate(String userId, String prefKey, String prefValue) {
        Optional<PreferenceEntity> existing = preferenceRepository.findByUserIdAndPrefKey(userId, prefKey);
        PreferenceEntity entity;
        if (existing.isPresent()) {
            entity = existing.get();
            entity.setPrefValue(prefValue);
            entity.setUpdateTime(LocalDateTime.now());
        } else {
            entity = new PreferenceEntity();
            entity.setId(UUID.randomUUID().toString().replace("-", ""));
            entity.setUserId(userId);
            entity.setPrefKey(prefKey);
            entity.setPrefValue(prefValue);
            entity.setCreateTime(LocalDateTime.now());
        }
        return toRes(preferenceRepository.save(entity));
    }

    @Transactional
    public void batchSave(String userId, Map<String, String> preferences) {
        for (Map.Entry<String, String> entry : preferences.entrySet()) {
            saveOrUpdate(userId, entry.getKey(), entry.getValue());
        }
    }

    private PreferenceRes toRes(PreferenceEntity e) {
        PreferenceRes res = new PreferenceRes();
        res.setId(e.getId());
        res.setUserId(e.getUserId());
        res.setPrefKey(e.getPrefKey());
        res.setPrefValue(e.getPrefValue());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }
}