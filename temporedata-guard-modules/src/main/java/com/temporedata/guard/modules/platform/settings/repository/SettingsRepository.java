package com.temporedata.guard.modules.platform.settings.repository;

import com.temporedata.guard.modules.platform.settings.entity.SettingsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettingsRepository extends JpaRepository<SettingsEntity, String> {

    Optional<SettingsEntity> findByConfigKey(String configKey);

    List<SettingsEntity> findByGroupName(String groupName);
}