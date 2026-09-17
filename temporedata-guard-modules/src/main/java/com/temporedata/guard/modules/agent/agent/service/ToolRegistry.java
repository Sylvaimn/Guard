package com.temporedata.guard.modules.agent.agent.service;
import com.temporedata.guard.modules.agent.aianalysis.service.AiTraceService;
import com.temporedata.guard.modules.police.network.service.ContactAnalysisService;
import com.temporedata.guard.modules.agent.aianalysis.service.DynamicAuthzService;
import com.temporedata.guard.modules.police.network.service.FraudAnalysisService;
import com.temporedata.guard.modules.police.network.service.FundFlowService;
import com.temporedata.guard.modules.police.network.service.RiskScoreService;
import com.temporedata.guard.modules.police.network.service.SentimentAnalysisService;
import com.temporedata.guard.modules.police.track.service.TrackService;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.police.entitygraph.service.EntityGraphService;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.modules.police.watch.service.WatchService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 工具注册表（收敛版）：声明式注册智能分析可调用的工具。
 * 工具只读、可审计、带溯源；写操作一律走人工确认。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ToolRegistry {

    private final IntelligenceService intelligenceService;
    private final EntityGraphService entityGraphService;
    private final WatchService watchService;
    private final com.temporedata.guard.modules.police.track.service.TrackService trackService;
    private final com.temporedata.guard.modules.agent.aianalysis.service.DynamicAuthzService dynamicAuthzService;
    private final com.temporedata.guard.modules.agent.aianalysis.service.AiTraceService aiTraceService;
    private final com.temporedata.guard.modules.police.network.service.FundFlowService fundFlowService;
    private final com.temporedata.guard.modules.police.network.service.ContactAnalysisService contactAnalysisService;
    private final com.temporedata.guard.modules.police.network.service.RiskScoreService riskScoreService;
    private final com.temporedata.guard.modules.police.network.service.FraudAnalysisService fraudAnalysisService;
    private final com.temporedata.guard.modules.police.network.service.SentimentAnalysisService sentimentAnalysisService;

    /** 工具名 → 执行器(keyword 为用户输入, 返回结果 Map) */
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /** 工具定义(Java 11 兼容) */
    public static final class Tool {
        private final String name;
        private final String description;
        private final BiFunction<LoginUser, String, Map<String, Object>> executor;

        public Tool(String name, String description, BiFunction<LoginUser, String, Map<String, Object>> executor) {
            this.name = name;
            this.description = description;
            this.executor = executor;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }

        public Map<String, Object> execute(LoginUser user, String keyword) {
            return executor.apply(user, keyword);
        }
    }

    @javax.annotation.PostConstruct
    public void init() {
        // 实体情报检索
        register(new Tool("entity_search", "检索人员/车辆/案件等实体情报(姓名/身份证/车牌)",
                (user, kw) -> {
                    var hits = intelligenceService.search(kw, null, user);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("hits", hits);
                    r.put("count", hits.size());
                    return r;
                }));
        // 实体图谱关联
        register(new Tool("graph_expand", "展开某实体的关联关系图(同案/同源/使用)",
                (user, kw) -> {
                    var entities = intelligenceService.search(kw, null, user);
                    if (entities.isEmpty()) {
                        return Map.of("error", "未找到实体");
                    }
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("graph", entityGraphService.getGraph(entities.get(0).getEntityId(), 1, null, user.getTenantId()));
                    return r;
                }));
        // 布控战果统计
        register(new Tool("watch_status", "查询布控预警战果统计",
                (user, kw) -> Map.of("stats", watchService.stats(user.getTenantId()))));
        // 资金流追踪(反诈: 账户穿透)
        register(new Tool("fund_flow", "查询资金流向与账户穿透(反诈研判)",
                (user, kw) -> fundFlowService.analyzeByKeyword(kw, 3, user)));
        // 通联分析(通话/社交关系)
        register(new Tool("contact_analysis", "查询人员的通联关系与团伙(三度关联/关键人)",
                (user, kw) -> contactAnalysisService.analyzeByKeyword(kw, user)));
        // 涉诈专项研判
        register(new Tool("fraud_probe", "涉诈专项研判(风险+资金+标签+团伙)",
                (user, kw) -> fraudAnalysisService.analyze(kw, user)));
        // 舆情研判
        register(new Tool("sentiment", "舆情情感与敏感词研判",
                (user, kw) -> sentimentAnalysisService.analyze(kw)));
        // 重点人员风险评分
        register(new Tool("risk_score", "查询重点人员风险积分与等级(红黄橙)",
                (user, kw) -> {
                    var hits = intelligenceService.search(kw, null, user);
                    if (hits.isEmpty()) return Map.of("error", "未找到实体");
                    String entityId = hits.get(0).getEntityId();
                    return Map.of("entity", hits.get(0).getDisplayName(),
                            "risk", riskScoreService.detail(entityId, user.getTenantId()));
                }));
        // 轨迹分析(近30天活动规律/落脚点)
        register(new Tool("track_query", "查询人员/车辆的轨迹与活动规律(近30天)",
                (user, kw) -> {
                    var hits = intelligenceService.search(kw, null, user);
                    if (hits.isEmpty()) {
                        return Map.of("error", "未找到实体");
                    }
                    String entityId = hits.get(0).getEntityId();
                    var summary = trackService.summary(entityId,
                            java.time.LocalDateTime.now().minusDays(30),
                            java.time.LocalDateTime.now(), user.getTenantId());
                    return Map.of("entity", hits.get(0).getDisplayName(), "summary", summary);
                }));
    }

    public void register(Tool tool) {
        tools.put(tool.name(), tool);
    }

    public Map<String, Tool> all() {
        return tools;
    }

    /** 执行工具(权限前置校验 + 轨迹记录 + 未注册抛错) */
    public Map<String, Object> execute(String name, LoginUser user, String keyword, String sessionId) {
        Tool tool = tools.get(name);
        if (tool == null) {
            throw new BusinessException("未知工具: " + name);
        }
        // P2: 动态权限前置校验(密级门槛)
        dynamicAuthzService.check(user, name);
        log.info("[AI-TOOL] {} invoked by {} (keyword={})", name, user.getUsername(), keyword);
        long start = System.currentTimeMillis();
        Map<String, Object> result = tool.execute(user, keyword);
        // P2: 全链路轨迹
        aiTraceService.record(user.getUserId(), sessionId, "TOOL", name,
                Map.of("keyword", keyword == null ? "" : keyword, "costMs", System.currentTimeMillis() - start),
                Map.of("resultKeys", result.keySet()), user.getTenantId());
        return result;
    }
}
