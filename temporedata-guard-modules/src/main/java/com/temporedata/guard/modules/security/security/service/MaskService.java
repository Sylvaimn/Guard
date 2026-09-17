/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.security.MaskRuleReq
 *  com.temporedata.guard.api.security.MaskRuleRes
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.security.security.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.security.MaskRuleReq;
import com.temporedata.guard.api.security.MaskRuleRes;
import com.temporedata.guard.modules.security.security.entity.MaskRuleEntity;
import com.temporedata.guard.modules.security.security.repository.MaskRuleRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MaskService {
    private static final Logger log = LoggerFactory.getLogger(MaskService.class);
    private final MaskRuleRepository maskRuleRepository;

    public List<MaskRuleRes> list(String tenantId) {
        return this.maskRuleRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream().map(this::toRes).collect(Collectors.toList());
    }

    public MaskRuleRes detail(String id) {
        return this.toRes((MaskRuleEntity)this.maskRuleRepository.findById(id).orElseThrow(() -> new BusinessException("\u8131\u654f\u89c4\u5219\u4e0d\u5b58\u5728")));
    }

    @Transactional
    public MaskRuleRes create(MaskRuleReq req, String tenantId) {
        MaskRuleEntity entity = new MaskRuleEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setRuleType(req.getRuleType());
        entity.setMaskPattern(req.getMaskPattern());
        entity.setDescription(req.getDescription());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTableName(req.getTableName());
        entity.setColumnName(req.getColumnName());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return this.toRes((MaskRuleEntity)this.maskRuleRepository.save(entity));
    }

    @Transactional
    public MaskRuleRes update(String id, MaskRuleReq req) {
        MaskRuleEntity entity = (MaskRuleEntity)this.maskRuleRepository.findById(id).orElseThrow(() -> new BusinessException("\u8131\u654f\u89c4\u5219\u4e0d\u5b58\u5728"));
        entity.setName(req.getName());
        entity.setRuleType(req.getRuleType());
        entity.setMaskPattern(req.getMaskPattern());
        entity.setDescription(req.getDescription());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setTableName(req.getTableName());
        entity.setColumnName(req.getColumnName());
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 1);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((MaskRuleEntity)this.maskRuleRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        this.maskRuleRepository.deleteById(id);
    }

    @Transactional
    public MaskRuleRes toggle(String id) {
        MaskRuleEntity entity = this.maskRuleRepository.findById(id).orElseThrow(() -> new BusinessException("\u8131\u654f\u89c4\u5219\u4e0d\u5b58\u5728"));
        entity.setStatus(entity.getStatus() == 1 ? 0 : 1);
        entity.setUpdateTime(LocalDateTime.now());
        return this.toRes((MaskRuleEntity)this.maskRuleRepository.save(entity));
    }

    public String maskValue(String value, String ruleType) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        switch (ruleType) {
            case "PHONE": {
                return this.maskPhone(value);
            }
            case "EMAIL": {
                return this.maskEmail(value);
            }
            case "ID_CARD": {
                return this.maskIdCard(value);
            }
            case "NAME": {
                return this.maskName(value);
            }
            case "BANK_CARD": {
                return this.maskBankCard(value);
            }
            case "CUSTOM": {
                return "****";
            }
        }
        return value;
    }

    private String maskPhone(String phone) {
        if (phone.length() >= 11) {
            return phone.substring(0, 3) + "****" + phone.substring(7);
        }
        return "****";
    }

    private String maskEmail(String email) {
        int atIdx = email.indexOf(64);
        if (atIdx > 2) {
            return email.substring(0, 2) + "****" + email.substring(atIdx);
        }
        return "****@" + email.substring(atIdx + 1);
    }

    private String maskIdCard(String idCard) {
        if (idCard.length() >= 15) {
            return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
        }
        return "****";
    }

    private String maskName(String name) {
        if (name.length() == 1) {
            return "*";
        }
        if (name.length() == 2) {
            return name.substring(0, 1) + "*";
        }
        return name.substring(0, 1) + "*".repeat(name.length() - 2) + name.substring(name.length() - 1);
    }

    private String maskBankCard(String card) {
        if (card.length() >= 8) {
            return card.substring(0, 4) + " **** **** " + card.substring(card.length() - 4);
        }
        return "****";
    }

    private MaskRuleRes toRes(MaskRuleEntity e) {
        MaskRuleRes res = new MaskRuleRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setRuleType(e.getRuleType());
        res.setMaskPattern(e.getMaskPattern());
        res.setDescription(e.getDescription());
        res.setDatasourceId(e.getDatasourceId());
        res.setTableName(e.getTableName());
        res.setColumnName(e.getColumnName());
        res.setStatus(e.getStatus());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }

    public MaskService(MaskRuleRepository maskRuleRepository) {
        this.maskRuleRepository = maskRuleRepository;
    }
}

