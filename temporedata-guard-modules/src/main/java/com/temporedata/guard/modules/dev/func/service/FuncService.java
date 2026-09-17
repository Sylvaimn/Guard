package com.temporedata.guard.modules.dev.func.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.func.FuncReq;
import com.temporedata.guard.api.func.FuncRes;
import com.temporedata.guard.modules.dev.func.entity.FuncEntity;
import com.temporedata.guard.modules.dev.func.repository.FuncRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FuncService {

    private final FuncRepository funcRepository;

    public List<FuncRes> list(String tenantId) {
        return funcRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public FuncRes detail(String id) {
        return toRes(funcRepository.findById(id)
                .orElseThrow(() -> new BusinessException("函数不存在")));
    }

    @Transactional
    public FuncRes create(FuncReq req, String tenantId) {
        FuncEntity entity = new FuncEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setLanguage(req.getLanguage());
        entity.setScript(req.getScript());
        entity.setDescription(req.getDescription());
        entity.setStatus("ACTIVE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(funcRepository.save(entity));
    }

    @Transactional
    public FuncRes update(String id, FuncReq req) {
        FuncEntity entity = funcRepository.findById(id)
                .orElseThrow(() -> new BusinessException("函数不存在"));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setLanguage(req.getLanguage());
        entity.setScript(req.getScript());
        entity.setDescription(req.getDescription());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(funcRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        funcRepository.deleteById(id);
    }

    public String testFunc(String id) {
        FuncEntity entity = funcRepository.findById(id)
                .orElseThrow(() -> new BusinessException("函数不存在"));
        // Placeholder: validate function script syntax
        log.info("Testing function: {}", entity.getName());
        return "函数语法校验通过（占位实现）";
    }

    private FuncRes toRes(FuncEntity e) {
        FuncRes res = new FuncRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setLanguage(e.getLanguage());
        res.setDescription(e.getDescription());
        res.setStatus(e.getStatus());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }
}