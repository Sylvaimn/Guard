package com.temporedata.guard.modules.agent.agent.service;
import com.temporedata.guard.modules.agent.agent.entity.AgentConfigEntity;
import com.temporedata.guard.modules.agent.agent.repository.AgentConfigRepository;
import com.temporedata.guard.modules.agent.aianalysis.service.EntityCardService;
import com.temporedata.guard.modules.agent.aianalysis.service.EntitySlotTracker;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.modules.agent.aianalysis.service.MemoryService;
import com.temporedata.guard.modules.agent.agent.entity.ModelPolicyEntity;
import com.temporedata.guard.modules.agent.agent.repository.ModelPolicyRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.agent.AgentChatReq;
import com.temporedata.guard.api.agent.AgentChatRes;
import com.temporedata.guard.api.agent.AgentConfigReq;
import com.temporedata.guard.api.agent.AgentConfigRes;
import com.temporedata.guard.api.agent.AgentSessionRes;
import com.temporedata.guard.api.agent.AgentTestRes;
import com.temporedata.guard.modules.common.ai.entity.AiChatHistoryEntity;
import com.temporedata.guard.modules.common.ai.repository.AiChatHistoryRepository;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowRepository;
import com.temporedata.guard.common.util.SensitiveUtil;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Intelligent agent: LLM-powered chat with rule-based fallback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentService {

    private final DatasourceRepository datasourceRepository;
    private final WorkflowRepository workflowRepository;
    private final AgentConfigRepository agentConfigRepository;
    private final ModelPolicyRepository modelPolicyRepository;
    private final AiChatHistoryRepository chatHistoryRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;
    private final com.temporedata.guard.modules.police.intelligence.service.IntelligenceService intelligenceService;
    private final AnalysisAgent analysisAgent;
    private final AiModelGateway aiModelGateway;
    private final com.temporedata.guard.modules.agent.aianalysis.service.EntitySlotTracker entitySlotTracker;
    private final com.temporedata.guard.modules.agent.aianalysis.service.MemoryService memoryService;
    private final com.temporedata.guard.modules.agent.aianalysis.service.EntityCardService entityCardService;

    private static final String SYSTEM_PROMPT =
            "你是轻舟云大数据平台的 AI 助手。你可以帮助用户生成 SQL、推荐数据处理流程、解答数据平台相关问题。" +
            "回答简洁专业，涉及代码时使用 markdown 代码块。";

    // Common SQL patterns for natural language mapping
    private static final Pattern COUNT_PATTERN = Pattern.compile("(统计|查询|多少|数量|count).*?(表|table)\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SELECT_PATTERN = Pattern.compile("(查询|查看|列出|显示|select).*?(表|table)\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOP_PATTERN = Pattern.compile("(前|top|最大的|最小的|最高的|最低的)\\s*(\\d+).*?(表|table)\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern GROUP_PATTERN = Pattern.compile("(按|分组|group by|每).*?(表|table)\\s*(\\w+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WORKFLOW_PATTERN = Pattern.compile("(工作流|编排|workflow|流程|任务流|dag)", Pattern.CASE_INSENSITIVE);

    // ==================== LLM Config ====================

    public AgentConfigRes getConfig(String tenantId) {
        return toConfigRes(agentConfigRepository.findByTenantId(tenantId).orElse(null));
    }

    @Transactional
    public AgentConfigRes saveConfig(AgentConfigReq req, String tenantId) {
        AgentConfigEntity entity = agentConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> {
                    AgentConfigEntity e = new AgentConfigEntity();
                    e.setId(UUID.randomUUID().toString().replace("-", ""));
                    e.setTenantId(tenantId);
                    e.setCreateTime(LocalDateTime.now());
                    return e;
                });
        entity.setProvider(req.getProvider());
        entity.setBaseUrl(req.getBaseUrl());
        if (req.getApiKey() != null && !req.getApiKey().trim().isEmpty()
                && !req.getApiKey().contains("****")) {
            entity.setApiKey(req.getApiKey().trim());
        }
        entity.setModel(req.getModel());
        entity.setTemperature(req.getTemperature() != null ? req.getTemperature() : 0.7);
        entity.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        entity.setUpdateTime(LocalDateTime.now());
        return toConfigRes(agentConfigRepository.save(entity));
    }

    /** 系统级模型策略(默认模型/参数约束/锁定开关/路由/配额), 所有登录用户可读 */
    public Map<String, Object> getModelPolicy() {
        ModelPolicyEntity entity = modelPolicyRepository.findTopByOrderByUpdateTimeDesc().orElse(null);
        if (entity == null) {
            return defaultModelPolicy();
        }
        try {
            return objectMapper.readValue(entity.getPolicyJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("model policy parse failed, use default: {}", e.getMessage());
            return defaultModelPolicy();
        }
    }

    /** 保存系统级模型策略(仅管理员) */
    @Transactional
    public Map<String, Object> saveModelPolicy(Map<String, Object> policy) {
        try {
            String json = objectMapper.writeValueAsString(policy);
            ModelPolicyEntity entity = modelPolicyRepository.findTopByOrderByUpdateTimeDesc().orElseGet(() -> {
                ModelPolicyEntity e = new ModelPolicyEntity();
                e.setId(UUID.randomUUID().toString().replace("-", ""));
                e.setCreateTime(LocalDateTime.now());
                return e;
            });
            entity.setPolicyJson(json);
            entity.setUpdateTime(LocalDateTime.now());
            modelPolicyRepository.save(entity);
            return policy;
        } catch (Exception e) {
            throw new RuntimeException("模型策略保存失败: " + e.getMessage());
        }
    }

    private Map<String, Object> defaultModelPolicy() {
        Map<String, Object> policy = new java.util.LinkedHashMap<>();
        List<Map<String, Object>> models = new ArrayList<>();
        models.add(modelItem("qwen14b-police", "警务微调版-14B", "v1.2", "警务微调", "擅长案情摘要与研判", "在线"));
        models.add(modelItem("deepseek-v3", "DeepSeek-V3", "v3.0", "通用", "速度快、通用性强", "在线"));
        models.add(modelItem("qwen-plus", "轻量快速版", "v2.1", "轻量", "响应快、节省资源", "在线"));
        models.add(modelItem("qwen-vl", "多模态版", "v1.0", "多模态", "图片/视频理解，需多模态部署", "在线"));
        models.add(modelItem("qwen-long", "长文本版", "v1.0", "长文本", "长文档与报告生成", "在线"));
        policy.put("models", models);
        policy.put("globalDefault", "qwen14b-police");
        Map<String, Object> sceneDefaults = new java.util.LinkedHashMap<>();
        sceneDefaults.put("report", "qwen-long");
        sceneDefaults.put("photo", "qwen-vl");
        policy.put("sceneDefaults", sceneDefaults);
        Map<String, Object> roleDefaults = new java.util.LinkedHashMap<>();
        roleDefaults.put("POLICE", "qwen14b-police");
        roleDefaults.put("EXPERT", "deepseek-v3");
        policy.put("roleDefaults", roleDefaults);
        Map<String, Object> caseDefaults = new java.util.LinkedHashMap<>();
        caseDefaults.put("fraud", "qwen14b-police");
        caseDefaults.put("drug", "deepseek-v3");
        policy.put("caseDefaults", caseDefaults);
        Map<String, Object> limits = new java.util.LinkedHashMap<>();
        limits.put("temperature", limitItem(0.0, 0.5));
        limits.put("maxTokens", limitItem(512, 4096));
        policy.put("limits", limits);
        Map<String, Object> locks = new java.util.LinkedHashMap<>();
        locks.put("rag", true);
        locks.put("toolCalls", false);
        policy.put("locks", locks);
        List<Map<String, Object>> routes = new ArrayList<>();
        routes.add(routeItem("报告", "qwen-long"));
        routes.add(routeItem("照片|图片|以图", "qwen-vl"));
        policy.put("routes", routes);
        Map<String, Object> quota = new java.util.LinkedHashMap<>();
        quota.put("dailyCalls", 500);
        quota.put("dailyTokens", 1000000);
        policy.put("quota", quota);
        return policy;
    }

    private Map<String, Object> modelItem(String id, String name, String version, String type, String desc, String status) {
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("version", version);
        m.put("type", type);
        m.put("desc", desc);
        m.put("status", status);
        return m;
    }

    private Map<String, Object> limitItem(double min, double max) {
        Map<String, Object> l = new java.util.LinkedHashMap<>();
        l.put("min", min);
        l.put("max", max);
        return l;
    }

    private Map<String, Object> routeItem(String keyword, String model) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("keyword", keyword);
        r.put("model", model);
        return r;
    }

    public AgentTestRes testConnection(AgentConfigReq req) {
        String baseUrl = normalizeBaseUrl(req.getBaseUrl());
        String apiKey = req.getApiKey();
        if (apiKey != null && apiKey.contains("****") && req.getModel() == null) {
            return fail("请填写完整的 API Key 后再测试");
        }
        try {
            String reply = callLlm(baseUrl, apiKey, req.getModel(),
                    Arrays.asList(systemMessage(), userMessage("你好，请回复：连接成功。")),
                    req.getTemperature() != null ? req.getTemperature() : 0.7);
            return ok("连接成功，模型返回：" + (reply != null ? reply.substring(0, Math.min(60, reply.length())) : ""));
        } catch (Exception e) {
            log.warn("LLM test failed: {}", e.getMessage());
            return fail("连接失败：" + e.getMessage());
        }
    }

    private AgentConfigRes toConfigRes(AgentConfigEntity e) {
        AgentConfigRes res = new AgentConfigRes();
        if (e == null) {
            res.setProvider("CUSTOM");
            res.setEnabled(false);
            res.setApiKeySet(false);
            res.setTemperature(0.7);
            return res;
        }
        res.setProvider(e.getProvider());
        res.setBaseUrl(e.getBaseUrl());
        res.setModel(e.getModel());
        res.setTemperature(e.getTemperature());
        res.setEnabled(e.getEnabled() != null && e.getEnabled());
        res.setApiKeySet(e.getApiKey() != null && !e.getApiKey().isEmpty());
        if (res.getApiKeySet()) {
            String key = e.getApiKey();
            res.setApiKeyMasked(key.length() > 8
                    ? key.substring(0, 4) + "****" + key.substring(key.length() - 4)
                    : "****");
        }
        return res;
    }

    private AgentTestRes ok(String message) {
        AgentTestRes res = new AgentTestRes();
        res.setOk(true);
        res.setMessage(message);
        return res;
    }

    private AgentTestRes fail(String message) {
        AgentTestRes res = new AgentTestRes();
        res.setOk(false);
        res.setMessage(message);
        return res;
    }

    // ==================== Chat ====================

    public AgentChatRes chat(AgentChatReq req, String tenantId, String userId, String density) {
        io.micrometer.core.instrument.Counter.builder("ai_chat_total").description("Agent 对话调用量")
                .tag("tenant", String.valueOf(tenantId)).register(meterRegistry).increment();
        String message = req.getMessage();
        AgentChatRes res = new AgentChatRes();

        if (message == null || message.trim().isEmpty()) {
            res.setReply("你好！我是轻舟云 AI 助手，可以帮你：\n1. 生成SQL查询\n2. 推荐工作流编排\n3. 分析数据质量\n请问有什么可以帮你的？");
            res.setReply(SensitiveUtil.maskText(res.getReply()));
            res.setAction("GENERAL");
            res.setSuggestions(Arrays.asList("帮我生成查询SQL", "推荐一个数据处理流程", "检查数据质量"));
            return res;
        }

        // 记忆注入 + 指代消解(P0): 解析"他/该案"等指代; 新会话注入长期记忆提示
        String resolvedMessage = message;
        String sessionId0 = req.getSessionId();
        if (message != null && !message.isBlank()) {
            resolvedMessage = entitySlotTracker.resolve(sessionId0, message);
            if (!resolvedMessage.equals(message)) {
                log.debug("[MEMORY] pronoun resolved: {} -> {}", message, resolvedMessage);
            }
        }

        // 统一编排: 先走工具意图(实体情报/图谱/战果), 无工具意图时走原有 LLM/规则逻辑
        if (resolvedMessage != null && !resolvedMessage.isBlank()) {
            java.util.Map<String, Object> toolResult = null;
            try {
                toolResult = analysisAgent.tryAnalyze(tenantId, userId, density, resolvedMessage);
            } catch (BusinessException be) {
                // 工具密级不足: 降级走 LLM/规则, 保证对话可用
                log.warn("[AGENT] tool analyze denied ({}), fallback to LLM/rule: {}", be.getMessage(), resolvedMessage);
            }
            if (toolResult != null) {
                // 观察槽位(工具命中实体)
                Object results = toolResult.get("results");
                Object hits = results instanceof java.util.Map ? ((java.util.Map<?, ?>) results).get("hits") : null;
                if (hits instanceof java.util.List && !((java.util.List<?>) hits).isEmpty()) {
                    entitySlotTracker.observe(sessionId0,
                            ((java.util.List<?>) hits).stream().limit(3)
                                    .map(h -> h instanceof com.temporedata.guard.api.intelligence.IntelligenceRes
                                            ? ((com.temporedata.guard.api.intelligence.IntelligenceRes) h).getDisplayName()
                                            : String.valueOf(h))
                                    .collect(java.util.stream.Collectors.toList()));
                }
                res.setAction("ANALYZE");
                res.setLlm(false);
                res.setReply(formatToolReply(toolResult));
            res.setReply(SensitiveUtil.maskText(res.getReply()));
                res.setToolResult(toolResult.get("results"));
                // 实体卡片下发(原型: 查看详情结构化展示)
                try {
                    LoginUser cardUser = resolveLoginUser(userId, tenantId);
                    res.setEntityCard(buildEntityCard(toolResult, cardUser));
                } catch (Exception e) {
                    log.debug("EntityCard build skipped: {}", e.getMessage());
                }
                String sessionId = req.getSessionId() != null ? req.getSessionId()
                        : UUID.randomUUID().toString().replace("-", "");
                saveMessage(userId, sessionId, "USER", resolvedMessage, null, tenantId);
                saveMessage(userId, sessionId, "ASSISTANT", res.getReply(), null, tenantId);
                res.setSessionId(sessionId);
                return res;
            }
        }

        AgentConfigEntity config = agentConfigRepository.findByTenantId(tenantId)
                .filter(c -> c.getEnabled() != null && c.getEnabled()
                        && c.getBaseUrl() != null && c.getApiKey() != null)
                .orElse(null);

        if (config != null) {
            return llmChat(req, config, tenantId, userId);
        }

        // 未接入大模型: 不做伪智能应答。工具型问题(情报/图谱/布控等)已在上方真实执行;
        // 其余对话明确提示接入模型, 不编造回答(真实 Agent 设计: 无模型不参与演示)。
        res = new AgentChatRes();
        res.setLlm(false);
        res.setAction("MODEL_REQUIRED");
        res.setReply("当前未接入大模型：请进入设置 ⚙️ → 大模型配置，填写模型 API（或接入内网私有化模型）后开启智能分析。\n\n"
                + "已接入模型前，可继续使用以下数据能力（无需大模型）：\n"
                + "- 情报检索/对象研判/关系图谱/布控（查一下XX、XX的关系）\n"
                + "- 数据查询/专案/线索 等工具指令（将真实返回数据）");
        String sessionId = req.getSessionId() != null ? req.getSessionId()
                : UUID.randomUUID().toString().replace("-", "");
        saveMessage(userId, sessionId, "USER", message, null, tenantId);
        saveMessage(userId, sessionId, "ASSISTANT", res.getReply(), null, tenantId);
        res.setSessionId(sessionId);
        return res;
    }

    private LoginUser resolveLoginUser(String userId, String tenantId) {
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser) {
            return (LoginUser) auth.getPrincipal();
        }
        return new LoginUser(userId, "user", null, tenantId, "INTERNAL", java.util.Set.of("USER"), true);
    }

    /** 从链式结果提取首个实体生成卡片 */
    private com.temporedata.guard.api.agent.EntityCardRes buildEntityCard(java.util.Map<String, Object> toolResult,
                                                                    LoginUser user) {
        Object results = toolResult.get("results");
        Object hits = results instanceof java.util.Map ? ((java.util.Map<?, ?>) results).get("hits") : null;
        String entityId = null;
        if (hits instanceof java.util.List && !((java.util.List<?>) hits).isEmpty()) {
            Object first = ((java.util.List<?>) hits).get(0);
            if (first instanceof com.temporedata.guard.api.intelligence.IntelligenceRes) {
                entityId = ((com.temporedata.guard.api.intelligence.IntelligenceRes) first).getEntityId();
            }
        }
        if (entityId == null) return null;
        return entityCardService.build(entityId, user);
    }

    /** 工具结果 → 可读文本 */
    private String formatToolReply(java.util.Map<String, Object> toolResult) {
        Object results = toolResult.get("results");
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(toolResult.get("tool")).append("】执行完成\n");
        if (toolResult.get("summary") != null) {
            sb.append(toolResult.get("summary")).append("\n");
        }
        if (results instanceof java.util.Map) {
            java.util.Map<?, ?> m = (java.util.Map<?, ?>) results;
            if (m.get("count") != null) {
                sb.append("命中 ").append(m.get("count")).append(" 条\n");
            }
            if (m.get("hits") instanceof java.util.List) {
                for (Object hit : (java.util.List<?>) m.get("hits")) {
                    sb.append("  - ").append(hit).append("\n");
                }
            }
            if (m.get("stats") != null) {
                sb.append("战果统计: ").append(m.get("stats")).append("\n");
            }
        }
        sb.append("\n> 以上为工具检索结果，请人工核对。详细分析请在右侧能力面板查看。");
        return sb.toString();
    }

    private void saveMessage(String userId, String sessionId, String role, String content,
                             String modelName, String tenantId) {
        AiChatHistoryEntity msg = new AiChatHistoryEntity();
        msg.setId(UUID.randomUUID().toString().replace("-", ""));
        msg.setUserId(userId);
        msg.setSessionId(sessionId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setModelName(modelName);
        msg.setTenantId(tenantId);
        msg.setScene("USER".equals(role) ? classifyScene(content) : null);
        msg.setReadFlag("USER".equals(role) || Boolean.TRUE.equals(msg.getReadFlag()));
        msg.setCreateTime(LocalDateTime.now());
        chatHistoryRepository.save(msg);
    }

    /** 会话场景分类(原型: 案件线索/人员检索/车辆查询/舆情/研判报告) */
    private String classifyScene(String content) {
        if (content == null) return "GENERAL";
        if (content.matches(".*(车辆|车牌|轨迹|卡口).*")) return "VEHICLE";
        if (content.matches(".*(舆情|网络).*")) return "PUBLIC_OPINION";
        if (content.matches(".*(报告|研判报告|汇总).*")) return "REPORT";
        if (content.matches(".*(案件|线索|警情).*")) return "CASE_CLUE";
        if (content.matches(".*(人员|身份证|嫌疑人|姓).*")) return "PERSON";
        return "GENERAL";
    }

    /** 会话置读(选中会话时调用) */
    @org.springframework.transaction.annotation.Transactional
    public void markSessionRead(String sessionId) {
        chatHistoryRepository.markReadBySession(sessionId);
    }

    @Transactional
    public AgentChatRes llmChat(AgentChatReq req, AgentConfigEntity config, String tenantId, String userId) {
        AgentChatRes res = new AgentChatRes();
        String sessionId = req.getSessionId() != null ? req.getSessionId()
                : UUID.randomUUID().toString().replace("-", "");

        // Save user message
        saveMessage(userId, sessionId, "USER", req.getMessage(),
                req.getModelName() != null ? req.getModelName() : config.getModel(), tenantId);

        try {
            List<Map<String, String>> messages = buildContextMessages(sessionId, userId, tenantId);
            String reply = callLlm(config.getBaseUrl(), config.getApiKey(),
                    req.getModelName() != null ? req.getModelName() : config.getModel(),
                    messages, config.getTemperature() != null ? config.getTemperature() : 0.7);

            saveMessage(userId, sessionId, "ASSISTANT", reply, config.getModel(), tenantId);

            res.setReply(reply);
            res.setReply(SensitiveUtil.maskText(res.getReply()));
            res.setSessionId(sessionId);
            res.setLlm(true);
            res.setAction("GENERAL");
            return res;
        } catch (Exception e) {
            log.warn("LLM chat failed: {}", e.getMessage());
            res.setReply("抱歉，调用大模型失败：" + e.getMessage() + "\n\n请检查右上角 ⚙️ 大模型配置是否正确。");
            res.setReply(SensitiveUtil.maskText(res.getReply()));
            res.setSessionId(sessionId);
            res.setLlm(true);
            res.setAction("GENERAL");
            return res;
        }
    }

    private List<Map<String, String>> buildContextMessages(String sessionId, String userId, String tenantId) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage());
        List<AiChatHistoryEntity> history = chatHistoryRepository
                .findBySessionIdOrderByCreateTimeAsc(sessionId);
        for (AiChatHistoryEntity h : history) {
            messages.add(message("USER".equals(h.getRole()) ? "user" : "assistant", h.getContent()));
        }
        return messages;
    }

    private Map<String, String> systemMessage() {
        return message("system", SYSTEM_PROMPT);
    }

    private Map<String, String> userMessage(String content) {
        return message("user", content);
    }

    private Map<String, String> message(String role, String content) {
        Map<String, String> m = new HashMap<>();
        m.put("role", role);
        m.put("content", content);
        return m;
    }

    private String callLlm(String baseUrl, String apiKey, String model,
                           List<Map<String, String>> messages, double temperature) throws Exception {
        String url = normalizeBaseUrl(baseUrl) + "/chat/completions";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("messages", messages);
        body.put("temperature", temperature);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (apiKey != null && !apiKey.isEmpty()) {
            headers.setBearerAuth(apiKey);
        }

        HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
        String raw = restTemplate.postForObject(url, entity, String.class);
        JsonNode root = objectMapper.readTree(raw);
        JsonNode choice = root.path("choices").path(0);
        return choice.path("message").path("content").asText();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) return "";
        String url = baseUrl.trim();
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.endsWith("/chat/completions")) url = url.substring(0, url.length() - "/chat/completions".length());
        return url;
    }

    // ==================== Sessions ====================

    public List<AgentSessionRes> listSessions(String userId) {
        List<AiChatHistoryEntity> all = chatHistoryRepository.findByUserIdOrderByCreateTimeDesc(userId);
        Map<String, List<AiChatHistoryEntity>> grouped = all.stream()
                .filter(h -> h.getSessionId() != null)
                .collect(Collectors.groupingBy(AiChatHistoryEntity::getSessionId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            list.sort((a, b) -> a.getCreateTime().compareTo(b.getCreateTime()));
                            return list;
                        })));
        return grouped.entrySet().stream()
                .map(e -> {
                    List<AiChatHistoryEntity> msgs = e.getValue();
                    AgentSessionRes res = new AgentSessionRes();
                    res.setSessionId(e.getKey());
                    String first = msgs.get(0).getContent();
                    res.setTitle(first.length() > 24 ? first.substring(0, 24) + "…" : first);
                    res.setMessageCount(msgs.size());
                    res.setScene(classifyScene(msgs.get(0).getContent()));
                    res.setUnreadCount((int) chatHistoryRepository
                            .countBySessionIdAndRoleAndReadFlagFalse(e.getKey(), "ASSISTANT"));
                    res.setLastTime(msgs.get(msgs.size() - 1).getCreateTime() != null
                            ? msgs.get(msgs.size() - 1).getCreateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
                            : "");
                    return res;
                })
                .sorted((a, b) -> b.getLastTime().compareTo(a.getLastTime()))
                .collect(Collectors.toList());
    }

    public List<Map<String, String>> history(String sessionId) {
        return chatHistoryRepository.findBySessionIdOrderByCreateTimeAsc(sessionId).stream()
                .map(h -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("role", "USER".equals(h.getRole()) ? "user" : "assistant");
                    m.put("content", h.getContent());
                    m.put("time", h.getCreateTime() != null ? h.getCreateTime().toString() : "");
                    return m;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSession(String sessionId) {
        chatHistoryRepository.deleteAll(chatHistoryRepository.findBySessionIdOrderByCreateTimeAsc(sessionId));
    }

    // ==================== Rule-based fallback ====================

    private String extractTableName(String message) {
        Matcher m = COUNT_PATTERN.matcher(message);
        if (m.find()) return m.group(3);
        m = SELECT_PATTERN.matcher(message);
        if (m.find()) return m.group(3);
        m = GROUP_PATTERN.matcher(message);
        if (m.find()) return m.group(3);
        return null;
    }
}