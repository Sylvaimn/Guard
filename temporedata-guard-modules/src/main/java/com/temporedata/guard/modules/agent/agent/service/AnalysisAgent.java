package com.temporedata.guard.modules.agent.agent.service;
import com.temporedata.guard.modules.agent.aianalysis.service.AiTraceService;
import com.temporedata.guard.modules.agent.aianalysis.service.GraphFactChecker;
import com.temporedata.guard.modules.agent.aianalysis.service.PromptGuard;

import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 智能分析编排（P1 升级）：任务规划(多步链) → 工具执行 → 反思核查(Reflector)。
 * - 规则规划: 组合意图(如"查X情报并展开图谱") → [entity_search, graph_expand] 链式执行
 * - 工具执行只读; 写操作一律输出"建议动作"供人工确认
 * - Reflector: 对链式结果做图谱事实核查, 输出"已核实/待核实"标注
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisAgent {

    private final io.micrometer.core.instrument.MeterRegistry meterRegistry;

    private final ToolRegistry toolRegistry;
    private final AiModelGateway aiModelGateway;
    private final com.temporedata.guard.modules.agent.aianalysis.service.GraphFactChecker graphFactChecker;
    private final com.temporedata.guard.modules.agent.aianalysis.service.AiTraceService aiTraceService;
    private final com.temporedata.guard.modules.agent.aianalysis.service.PromptGuard promptGuard;

    /** 意图 → 工具映射(关键词命中任一即匹配, 按优先级排列) */
    private static final Map<Pattern, String> INTENT_RULES = new LinkedHashMap<>();

    static {
        INTENT_RULES.put(Pattern.compile(".*(战果|布控|预警|态势统计|处置).*"), "watch_status");
        INTENT_RULES.put(Pattern.compile(".*(涉诈|涉骗|反诈).*"), "fraud_probe");
        INTENT_RULES.put(Pattern.compile(".*(舆情|民意|网络言论).*"), "sentiment");
        INTENT_RULES.put(Pattern.compile(".*(风险|评分|管控等级).*"), "risk_score");
        INTENT_RULES.put(Pattern.compile(".*(资金|转账|汇款|流水|账户).*"), "fund_flow");
        INTENT_RULES.put(Pattern.compile(".*(通联|通话|联系|通讯|社交).*"), "contact_analysis");
        INTENT_RULES.put(Pattern.compile(".*(轨迹|追踪|行踪|活动规律|落脚点).*"), "track_query");
        INTENT_RULES.put(Pattern.compile(".*(图谱|关系图|关联关系).*"), "graph_expand");
        INTENT_RULES.put(Pattern.compile(".*(情报|查一下|查查|检索|研判|分析一下|信息).*"), "entity_search");
    }

    private static final Pattern SEARCH_INTENT = Pattern.compile(".*(情报|查一下|查查|检索|研判|分析一下).*");
    private static final Pattern TRACK_INTENT = Pattern.compile(".*(轨迹|追踪|行踪|活动规律|落脚点).*");
    private static final Pattern GRAPH_INTENT = Pattern.compile(".*(图谱|关联关系|关系图).*");

    /**
     * 供 chat 统一编排: 工具意图命中时返回结果(带 source), 否则 null 走原有 LLM/规则逻辑。
     */
    public Map<String, Object> tryAnalyze(String tenantId, String userId, String question) {
        return tryAnalyze(tenantId, userId, "INTERNAL", question);
    }

    /** 支持真实密级: 高密级工具(轨迹/图谱 CONFIDENTIAL)可由高权限用户调用 */
    public Map<String, Object> tryAnalyze(String tenantId, String userId, String density, String question) {
        LoginUser user = new LoginUser(userId, "user", null, tenantId,
                density == null || density.isBlank() ? "INTERNAL" : density,
                java.util.Set.of("USER"), true);
        Map<String, Object> out = analyze(user, question);
        Object tool = out.get("tool");
        if (tool != null && !"none".equals(String.valueOf(tool))) {
            try {
                meterRegistry.counter("temporedata_guard_agent_tool_call", "tool", String.valueOf(tool),
                        "tenant", String.valueOf(tenantId)).increment();
            } catch (Exception ignored) {
            }
        }
        if ("none".equals(out.get("tool"))) {
            return null;
        }
        return out;
    }

    /**
     * 执行研判(P1): 规划工具链 → 逐工具执行 → Reflector 核查 → 可选 LLM 总结。
     * 返回 {tool, chain[], steps[], verification, results, summary?}
     */
    public Map<String, Object> analyze(LoginUser user, String question) {
        // P2: 输入注入防护(先于意图识别, 命中注入模式直接拒绝)
        String safeQuestion = promptGuard.sanitize(question);
        if (safeQuestion == null) {
            aiTraceService.record(user.getUserId(), null, "PLAN", "blocked_injection",
                    Map.of("step", "INPUT_FILTER"), null, user.getTenantId());
            return Map.of("tool", "none", "reply", "输入包含疑似指令注入内容，已拦截。请正常描述研判需求。");
        }
        question = safeQuestion;
        String toolName = resolveIntent(question);
        if (toolName == null) {
            return Map.of("tool", "none", "reply", "未识别到可执行的研判意图，可尝试：查一下X的情报 / 分析X的关联 / 布控战果统计");
        }
        String keyword = extractKeyword(question);
        List<String> chain = planChain(question, keyword);
        aiTraceService.record(user.getUserId(), null, "PLAN", "intent",
                Map.of("question", question, "chain", chain), null, user.getTenantId());

        // 链式执行: 每步记录 {tool, keyword, results}
        List<Map<String, Object>> steps = new ArrayList<>();
        Map<String, Object> lastResults = Map.of();
        for (String stepTool : chain) {
            // Agent 权限继承: 工具所需权限 + 密级校验(用户仅警务工具集)
            com.temporedata.guard.modules.agent.aianalysis.security.AgentSecurityGuard.requireTool(
                    stepTool, user.getRoleCodes(), null);
            String stepKeyword = "watch_status".equals(stepTool) ? question : keyword;
            Map<String, Object> stepResults = toolRegistry.execute(stepTool, user, stepKeyword, null);
            steps.add(Map.of("tool", stepTool, "keyword", stepKeyword, "results", stepResults));
            lastResults = stepResults;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("tool", chain.size() > 1 ? "chain" : chain.get(0));
        out.put("chain", chain);
        out.put("steps", steps);
        out.put("results", lastResults);
        out.put("source", Map.of("tools", chain, "keyword", keyword));

        // Reflector: 图谱事实核查(基于链式命中实体)
        try {
            Map<String, Object> verification = graphFactChecker.verify(user.getTenantId(), chain, steps);
            out.put("verification", verification);
            if (verification.get("note") != null) {
                out.put("verifyNote", verification.get("note"));
            }
        } catch (Exception e) {
            log.warn("Reflector check skipped: {}", e.getMessage());
        }

        // 可选: LLM 总结
        try {
            String summary = aiModelGateway.chat(user.getTenantId(), user.getDensity(), "analysis-summary",
                    java.util.List.of(
                            Map.of("role", "system", "content", "你是警务研判助手。基于工具结果给出简洁结论，标注建议人工复核处，不编造数据。"),
                            Map.of("role", "user", "content", "问题：" + question + "\n工具结果：" + String.valueOf(lastResults).substring(0, Math.min(800, String.valueOf(lastResults).length())))));
            if (summary != null) {
                out.put("summary", summary);
            }
        } catch (Exception e) {
            log.warn("AI summary skipped: {}", e.getMessage());
        }
        return out;
    }

    /** 规则规划(P1): 组合意图 → 多步工具链 */
    private List<String> planChain(String question, String keyword) {
        List<String> chain = new ArrayList<>();
        boolean hasSearch = SEARCH_INTENT.matcher(question).matches();
        boolean hasTrack = TRACK_INTENT.matcher(question).matches();
        boolean hasGraph = GRAPH_INTENT.matcher(question).matches();
        if (hasTrack) {
            chain.add("track_query");
            return chain;
        }
        if (hasGraph) {
            if (hasSearch) chain.add("entity_search");
            chain.add("graph_expand");
            return chain;
        }
        // 专项目标优先(资金/通联/战果等); 否则实体检索
        String toolName = resolveIntent(question);
        if (toolName != null && !"entity_search".equals(toolName)) {
            chain.add(toolName);
            return chain;
        }
        if (hasSearch) {
            chain.add("entity_search");
            return chain;
        }
        if (toolName != null) chain.add(toolName);
        return chain;
    }

    /** 规则意图识别(可扩展 LLM 意图分类) */
    private String resolveIntent(String question) {
        for (Map.Entry<Pattern, String> e : INTENT_RULES.entrySet()) {
            if (e.getKey().matcher(question).matches()) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 抽取实体目标: 迭代剥离意图词/连接词/尾词 */
    private String extractKeyword(String question) {
        String kw = question
                .replaceFirst("^(查一下|分析一下|看一下|查查|帮我|请问|检索|分析|看看|查|找)", "")
                .replaceAll("[\"“”'\\s，。？?！!]?", "")
                .trim();
        String prev;
        do {
            prev = kw;
            kw = kw.replaceAll("(的情报|的关联|的相关|并展开|展开一下|展开|的图谱|关联关系|关联|关系|图谱|情报|信息|战果|统计|布控|预警|人员|相关人员|轨迹|行踪|活动规律|落脚点|通联|通话|通讯|联系|资金|转账|汇款|流水|账户|流向|风险|评分|等级|管控|涉诈|涉骗|反诈|舆情|研判|的)$", "");
        } while (!kw.equals(prev));
        return kw.length() >= 2 ? kw : question.trim();
    }
}
