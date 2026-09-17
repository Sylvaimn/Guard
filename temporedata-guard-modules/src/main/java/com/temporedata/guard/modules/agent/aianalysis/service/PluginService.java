/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 */
package com.temporedata.guard.modules.agent.aianalysis.service;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiToolPluginEntity;
import com.temporedata.guard.modules.agent.aianalysis.repository.AiToolPluginRepository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.agent.agent.service.ToolRegistry;


import com.temporedata.guard.security.LoginUser;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PluginService {
    private static final Logger log = LoggerFactory.getLogger(PluginService.class);
    private final AiToolPluginRepository pluginRepository;
    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public AiToolPluginEntity create(String name, String description, List<Map<String, String>> chain, String tenantId, String ownerId) {
        if (name == null || name.isBlank()) {
            throw new BusinessException("\u63d2\u4ef6\u540d\u5fc5\u586b");
        }
        if (chain == null || chain.isEmpty()) {
            throw new BusinessException("\u5de5\u5177\u94fe\u4e0d\u80fd\u4e3a\u7a7a");
        }
        for (Map<String, String> step : chain) {
            if (this.toolRegistry.all().containsKey(step.get("tool"))) continue;
            throw new BusinessException("\u672a\u77e5\u5de5\u5177: " + step.get("tool"));
        }
        AiToolPluginEntity p = new AiToolPluginEntity();
        p.setId(UUID.randomUUID().toString().replace("-", ""));
        p.setName(name);
        p.setDescription(description);
        try {
            p.setChain(this.objectMapper.writeValueAsString(chain));
        }
        catch (Exception e) {
            throw new BusinessException("\u5de5\u5177\u94fe\u683c\u5f0f\u9519\u8bef");
        }
        p.setTenantId(tenantId);
        p.setOwnerId(ownerId);
        p.setCreateTime(LocalDateTime.now());
        return (AiToolPluginEntity)this.pluginRepository.save(p);
    }

    public List<AiToolPluginEntity> list(String tenantId, String status) {
        return status != null && !status.isBlank() ? this.pluginRepository.findByTenantIdAndStatus(tenantId, status) : this.pluginRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    @Transactional
    public AiToolPluginEntity setStatus(String id, String tenantId, String status) {
        AiToolPluginEntity p = (AiToolPluginEntity)this.pluginRepository.findById(id).orElseThrow(() -> new BusinessException("\u63d2\u4ef6\u4e0d\u5b58\u5728"));
        if (!p.getTenantId().equals(tenantId)) {
            throw new BusinessException("\u65e0\u6743\u64cd\u4f5c\u8be5\u63d2\u4ef6");
        }
        p.setStatus(status);
        p.setUpdateTime(LocalDateTime.now());
        return (AiToolPluginEntity)this.pluginRepository.save(p);
    }

    @Transactional
    public void delete(String id, String tenantId) {
        AiToolPluginEntity p = (AiToolPluginEntity)this.pluginRepository.findById(id).orElseThrow(() -> new BusinessException("\u63d2\u4ef6\u4e0d\u5b58\u5728"));
        if (!p.getTenantId().equals(tenantId)) {
            throw new BusinessException("\u65e0\u6743\u64cd\u4f5c\u8be5\u63d2\u4ef6");
        }
        this.pluginRepository.delete(p);
    }

    public Map<String, Object> run(String id, String tenantId, LoginUser user, String keyword) {
        List<Map<String, String>> chain;
        AiToolPluginEntity p = (AiToolPluginEntity)this.pluginRepository.findById(id).orElseThrow(() -> new BusinessException("\u63d2\u4ef6\u4e0d\u5b58\u5728"));
        if (!p.getTenantId().equals(tenantId)) {
            throw new BusinessException("\u65e0\u6743\u6267\u884c\u8be5\u63d2\u4ef6");
        }
        if (!"ENABLED".equals(p.getStatus())) {
            throw new BusinessException("\u63d2\u4ef6\u5df2\u505c\u7528");
        }
        try {
            chain = (List)this.objectMapper.readValue(p.getChain(), (TypeReference)new TypeReference<List<Map<String, String>>>(){});
        }
        catch (Exception e) {
            throw new BusinessException("\u63d2\u4ef6\u5de5\u5177\u94fe\u635f\u574f");
        }
        ArrayList steps = new ArrayList();
        Map<String, Object> last = Map.of();
        for (Map<String, String> step : chain) {
            String tool = (String)step.get("tool");
            String kw = step.getOrDefault("keyword", keyword);
            last = this.toolRegistry.execute(tool, user, kw == null ? "" : kw, null);
            steps.add(Map.of("tool", tool, "keyword", kw, "results", last));
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("plugin", p.getName());
        out.put("chain", chain.stream().map(s -> (String)s.get("tool")).collect(Collectors.toList()));
        out.put("steps", steps);
        out.put("results", last);
        return out;
    }

    public PluginService(AiToolPluginRepository pluginRepository, ToolRegistry toolRegistry) {
        this.pluginRepository = pluginRepository;
        this.toolRegistry = toolRegistry;
    }
}

