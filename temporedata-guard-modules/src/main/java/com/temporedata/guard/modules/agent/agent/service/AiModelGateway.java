package com.temporedata.guard.modules.agent.agent.service;
import com.temporedata.guard.modules.agent.aianalysis.security.AiDensityGuard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.agent.agent.entity.AgentConfigEntity;
import com.temporedata.guard.modules.agent.agent.repository.AgentConfigRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 模型网关（收敛版）：统一模型调用入口。
 * - 读现有 AgentConfig（租户级单配置），无配置时返回 null 由调用方走规则兜底
 * - 密级隔离：外部模型拦截 INTERNAL 以上密级
 * - 降级链：主模型失败 → 抛错由调用方回退规则引擎
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiModelGateway {

    private final AgentConfigRepository agentConfigRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 场景路由配置: small/large/sql 场景 → 模型名(未配置则用主模型) */
    @org.springframework.beans.factory.annotation.Value("${zy.ai.route.small-model:}")
    private String smallModel;

    @org.springframework.beans.factory.annotation.Value("${zy.ai.route.large-model:}")
    private String largeModel;

    @org.springframework.beans.factory.annotation.Value("${zy.ai.route.sql-model:}")
    private String sqlModel;

    /** 场景温度策略 */
    private static final Map<String, Double> SCENE_TEMPERATURE = Map.of(
            "nl2sql", 0.1, "analysis-summary", 0.3, "intent", 0.0);

    /** 本地私有化模型地址关键字(ollama/vllm/localhost/127.0.0.1), 命中则视为本地模型 */
    private static final String[] LOCAL_MODEL_KEYWORDS = {"localhost", "127.0.0.1", "ollama", "vllm", "local"};

    public boolean isLocalModel(AgentConfigEntity config) {
        String baseUrl = config == null ? "" : config.getBaseUrl();
        if (baseUrl == null) return false;
        String url = baseUrl.toLowerCase();
        for (String kw : LOCAL_MODEL_KEYWORDS) {
            if (url.contains(kw)) return true;
        }
        return false;
    }

    /** 对话: 返回模型回复; 未配置模型时返回 null */
    public String chat(String tenantId, String userDensity, String scene,
                       List<Map<String, String>> messages) {
        AgentConfigEntity config = agentConfigRepository.findByTenantId(tenantId).orElse(null);
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())
                || config.getBaseUrl() == null || config.getBaseUrl().isBlank()) {
            log.debug("AI gateway: no model config for tenant {}", tenantId);
            return null;
        }
        boolean local = isLocalModel(config);
        AiDensityGuard.requireSendAllowed(userDensity, local, scene);
        try {
            // P3: 场景路由(模型名 + 温度)
            String routedModel = routeModel(scene, config);
            Double temperature = SCENE_TEMPERATURE.getOrDefault(scene, 0.3);
            log.debug("[AI-ROUTE] scene={} model={} temp={}", scene, routedModel, temperature);
            return callLlm(config, messages, routedModel, temperature);
        } catch (Exception e) {
            log.warn("AI gateway call failed (scene={}): {}", scene, e.getMessage());
            throw new BusinessException("模型调用失败: " + e.getMessage());
        }
    }

    /** 场景→模型名路由(yml 可配; 未配置回落主模型) */
    private String routeModel(String scene, AgentConfigEntity config) {
        String model = null;
        if ("nl2sql".equals(scene)) model = sqlModel;
        else if ("analysis-summary".equals(scene) || "intent".equals(scene)) model = smallModel;
        if (model == null || model.isBlank()) {
            model = config.getModel();
        }
        return model != null && !model.isBlank() ? model : "default";
    }

    private String callLlm(AgentConfigEntity config, List<Map<String, String>> messages,
                           String model, double temperature) throws Exception {
        String url = normalizeBaseUrl(config.getBaseUrl()) + "/chat/completions";
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (config.getApiKey() != null && !config.getApiKey().isEmpty()) {
            headers.setBearerAuth(config.getApiKey());
        }
        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        String raw = restTemplate.postForObject(url, entity, String.class);
        JsonNode root = objectMapper.readTree(raw);
        JsonNode choice = root.path("choices").path(0);
        return choice.path("message").path("content").asText();
    }

    private String normalizeBaseUrl(String baseUrl) {
        String url = baseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) url = url.substring(0, url.length() - "/chat/completions".length());
        return url;
    }
}
