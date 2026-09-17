package com.temporedata.guard.modules.common.driver.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.common.driver.entity.DriverEntity;
import com.temporedata.guard.modules.common.driver.repository.DriverRepository;
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
public class DriverService {

    private final DriverRepository driverRepository;

    public List<DriverEntity> listByTenant(String tenantId) {
        return driverRepository.findByTenantId(tenantId);
    }

    @Transactional
    public DriverEntity create(String name, String dbType, String version, String driverPath, String tenantId) {
        DriverEntity entity = new DriverEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setName(name);
        entity.setDbType(dbType);
        entity.setVersion(version);
        entity.setDriverPath(driverPath);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return driverRepository.save(entity);
    }

    @Transactional
    public void delete(String id) {
        driverRepository.deleteById(id);
    }
}