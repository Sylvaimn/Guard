package com.temporedata.guard.modules.platform.license.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.license.LicenseInstallReq;
import com.temporedata.guard.api.license.LicenseRes;
import com.temporedata.guard.modules.platform.license.entity.LicenseEntity;
import com.temporedata.guard.modules.platform.license.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * License management service: install, query, and validate license.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LicenseService {

    private final LicenseRepository licenseRepository;

    public LicenseRes getLicense(String tenantId) {
        LicenseEntity entity = licenseRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("未找到 License 信息"));
        return toRes(entity);
    }

    @Transactional
    public LicenseRes installLicense(LicenseInstallReq req, String tenantId) {
        licenseRepository.findByTenantId(tenantId).ifPresent(licenseRepository::delete);

        LicenseEntity entity = new LicenseEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setLicenseKey(req.getLicenseKey());
        entity.setStatus("ACTIVE");
        entity.setInstallDate(LocalDateTime.now());
        entity.setExpireDate(LocalDateTime.now().plusYears(1));
        entity.setTenantId(tenantId);
        return toRes(licenseRepository.save(entity));
    }

    public LicenseRes checkLicense(String tenantId) {
        LicenseEntity entity = licenseRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("未找到 License 信息"));
        if (entity.getExpireDate() != null && entity.getExpireDate().isBefore(LocalDateTime.now())) {
            entity.setStatus("EXPIRED");
            licenseRepository.save(entity);
        }
        return toRes(entity);
    }

    private LicenseRes toRes(LicenseEntity e) {
        LicenseRes res = new LicenseRes();
        res.setId(e.getId());
        res.setLicenseKey(e.getLicenseKey());
        res.setStatus(e.getStatus());
        res.setExpireDate(e.getExpireDate() != null ? e.getExpireDate().toString() : null);
        res.setInstallDate(e.getInstallDate() != null ? e.getInstallDate().toString() : null);
        return res;
    }
}