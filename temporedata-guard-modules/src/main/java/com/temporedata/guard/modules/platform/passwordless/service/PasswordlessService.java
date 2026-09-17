package com.temporedata.guard.modules.platform.passwordless.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.passwordless.PasswordlessConfigRes;
import com.temporedata.guard.modules.platform.passwordless.entity.PasswordlessConfigEntity;
import com.temporedata.guard.modules.platform.passwordless.repository.PasswordlessConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Passwordless login service: config management, send and verify verification code.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordlessService {

    private final PasswordlessConfigRepository passwordlessConfigRepository;

    public List<PasswordlessConfigRes> listAll(String tenantId) {
        return passwordlessConfigRepository.findAll().stream()
                .filter(e -> tenantId.equals(e.getTenantId()))
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public PasswordlessConfigRes getByLoginType(String loginType, String tenantId) {
        PasswordlessConfigEntity entity = passwordlessConfigRepository
                .findByLoginTypeAndTenantId(loginType, tenantId)
                .orElseThrow(() -> new BusinessException("Passwordless config not found"));
        return toRes(entity);
    }

    @Transactional
    public PasswordlessConfigRes create(PasswordlessConfigEntity entity) {
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setStatus("ENABLED");
        entity.setCreateTime(LocalDateTime.now());
        return toRes(passwordlessConfigRepository.save(entity));
    }

    @Transactional
    public PasswordlessConfigRes update(String id, PasswordlessConfigEntity entity) {
        PasswordlessConfigEntity existing = passwordlessConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Passwordless config not found"));
        existing.setLoginType(entity.getLoginType());
        existing.setConfig(entity.getConfig());
        existing.setUpdateTime(LocalDateTime.now());
        return toRes(passwordlessConfigRepository.save(existing));
    }

    @Transactional
    public void delete(String id) {
        passwordlessConfigRepository.deleteById(id);
    }

    @Transactional
    public PasswordlessConfigRes enable(String id) {
        PasswordlessConfigEntity entity = passwordlessConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Passwordless config not found"));
        entity.setStatus("ENABLED");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(passwordlessConfigRepository.save(entity));
    }

    @Transactional
    public PasswordlessConfigRes disable(String id) {
        PasswordlessConfigEntity entity = passwordlessConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Passwordless config not found"));
        entity.setStatus("DISABLED");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(passwordlessConfigRepository.save(entity));
    }

    public void sendCode(String loginType, String target) {
        log.info("Sending verification code via {} to {}", loginType, target);
        // TODO: implement actual verification code sending logic (SMS/Email/OAuth/LDAP)
    }

    public boolean verifyCode(String loginType, String target, String code) {
        log.info("Verifying code via {} for {} with code {}", loginType, target, code);
        // TODO: implement actual verification code validation logic
        return true;
    }

    private PasswordlessConfigRes toRes(PasswordlessConfigEntity e) {
        PasswordlessConfigRes res = new PasswordlessConfigRes();
        res.setId(e.getId());
        res.setLoginType(e.getLoginType());
        res.setConfig(e.getConfig());
        res.setStatus(e.getStatus());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }
}