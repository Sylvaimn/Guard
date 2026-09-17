package com.temporedata.guard.modules.platform.secret.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.secret.SecretReq;
import com.temporedata.guard.api.secret.SecretRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.platform.secret.entity.SecretEntity;
import com.temporedata.guard.modules.platform.secret.repository.SecretRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Secret management service: CRUD with encryption/decryption.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecretService {

    private final SecretRepository secretRepository;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    public List<SecretRes> list(String tenantId) {
        return secretRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public SecretRes detail(String id) {
        return toRes(secretRepository.findById(id)
                .orElseThrow(() -> new BusinessException("密钥不存在")));
    }

    @Transactional
    public SecretRes create(SecretReq req, String tenantId) {
        SecretEntity entity = new SecretEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setKey(req.getKey());
        if (req.getValue() != null && !req.getValue().isEmpty()) {
            entity.setValue(CryptoUtil.encrypt(req.getValue(), cryptoKey));
        }
        entity.setDescription(req.getDescription());
        entity.setScope(req.getScope() != null ? req.getScope() : "GLOBAL");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(secretRepository.save(entity));
    }

    @Transactional
    public SecretRes update(String id, SecretReq req) {
        SecretEntity entity = secretRepository.findById(id)
                .orElseThrow(() -> new BusinessException("密钥不存在"));
        entity.setKey(req.getKey());
        if (req.getValue() != null && !req.getValue().isEmpty()) {
            entity.setValue(CryptoUtil.encrypt(req.getValue(), cryptoKey));
        }
        entity.setDescription(req.getDescription());
        entity.setScope(req.getScope());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(secretRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        secretRepository.deleteById(id);
    }

    private SecretRes toRes(SecretEntity e) {
        SecretRes res = new SecretRes();
        res.setId(e.getId());
        res.setKey(e.getKey());
        try {
            res.setValue(CryptoUtil.decrypt(e.getValue(), cryptoKey));
        } catch (Exception ex) {
            res.setValue("***");
        }
        res.setDescription(e.getDescription());
        res.setScope(e.getScope());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }
}