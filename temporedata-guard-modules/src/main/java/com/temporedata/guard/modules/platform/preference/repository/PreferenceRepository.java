package com.temporedata.guard.modules.platform.preference.repository;

import com.temporedata.guard.modules.platform.preference.entity.PreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PreferenceRepository extends JpaRepository<PreferenceEntity, String> {

    List<PreferenceEntity> findByUserId(String userId);

    Optional<PreferenceEntity> findByUserIdAndPrefKey(String userId, String prefKey);
}