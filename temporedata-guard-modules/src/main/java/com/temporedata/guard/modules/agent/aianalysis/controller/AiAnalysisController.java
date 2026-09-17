/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.fasterxml.jackson.core.type.TypeReference
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.access.prepost.PreAuthorize
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RequestParam
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.agent.aianalysis.controller;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiReportEntity;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiRuleEntity;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiTaskEntity;
import com.temporedata.guard.modules.agent.aianalysis.service.AiTaskService;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiToolPluginEntity;
import com.temporedata.guard.modules.agent.aianalysis.entity.AiTraceEntity;
import com.temporedata.guard.modules.agent.aianalysis.service.AiTraceService;
import com.temporedata.guard.modules.agent.agent.service.AnalysisAgent;
import com.temporedata.guard.modules.agent.aianalysis.entity.AnomalyClueEntity;
import com.temporedata.guard.modules.agent.aianalysis.service.AnomalyDetector;
import com.temporedata.guard.modules.police.network.service.DocTemplateService;
import com.temporedata.guard.modules.police.network.service.FraudAnalysisService;
import com.temporedata.guard.modules.agent.aianalysis.service.MemoryService;
import com.temporedata.guard.modules.agent.aianalysis.service.Nl2RuleService;
import com.temporedata.guard.modules.agent.aianalysis.nl2sql.Nl2SqlService;
import com.temporedata.guard.modules.agent.aianalysis.service.PluginService;
import com.temporedata.guard.modules.agent.aianalysis.rag.RagService;
import com.temporedata.guard.modules.agent.aianalysis.service.ReportComposeService;
import com.temporedata.guard.modules.agent.aianalysis.service.SemanticCacheService;
import com.temporedata.guard.modules.police.network.service.SentimentAnalysisService;
import com.temporedata.guard.modules.agent.agent.service.ToolRegistry;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.pojos.BaseResponse;
















import com.temporedata.guard.modules.security.security.audit.Audit;
import com.temporedata.guard.security.LoginUser;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/ai"})
public class AiAnalysisController {
    private final AnalysisAgent analysisAgent;
    private final ToolRegistry toolRegistry;
    private final Nl2SqlService nl2SqlService;
    private final RagService ragService;
    private final AnomalyDetector anomalyDetector;
    private final AiTaskService aiTaskService;
    private final MemoryService memoryService;
    private final SemanticCacheService semanticCacheService;
    private final PluginService pluginService;
    private final ReportComposeService reportComposeService;
    private final Nl2RuleService nl2RuleService;
    private final AiTraceService aiTraceService;
    private final FraudAnalysisService fraudAnalysisService;
    private final SentimentAnalysisService sentimentAnalysisService;
    private final DocTemplateService docTemplateService;

    @PostMapping(value={"/analyze"})
    @Audit(action="AI_ANALYZE", targetType="AI_AGENT", detail="\u7814\u5224: #{#body?.get('question')}")
    public BaseResponse<Map<String, Object>> analyze(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.analysisAgent.analyze(user, body.getOrDefault("question", "")));
    }

    @PostMapping(value={"/nl2sql"})
    @Audit(action="AI_ANALYZE", targetType="AI_NL2SQL", detail="\u95ee\u7b54: #{#body?.get('question')}")
    public BaseResponse<Map<String, Object>> nl2sql(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2SqlService.ask(body.get("datasourceId"), body.getOrDefault("question", ""), user));
    }

    @GetMapping(value={"/retrieve"})
    public BaseResponse<List<Map<String, Object>>> retrieve(@RequestParam String question, @AuthenticationPrincipal LoginUser user) {
        String fp = user.getDensity() == null ? "PUBLIC" : user.getDensity();
        String cached = this.semanticCacheService.get(user.getTenantId(), fp, question);
        if (cached != null) {
            try {
                List hits = (List)new ObjectMapper().readValue(cached, (TypeReference)new TypeReference<List<Map<String, Object>>>(){});
                return BaseResponse.success(hits);
            }
            catch (Exception hits) {
                // empty catch block
            }
        }
        List<Map<String, Object>> hits = this.ragService.retrieve(question, user.getTenantId(), user.getDensity());
        try {
            this.semanticCacheService.put(user.getTenantId(), fp, question, new ObjectMapper().writeValueAsString(hits));
        }
        catch (Exception exception) {
            // empty catch block
        }
        return BaseResponse.success(hits);
    }

    @PostMapping(value={"/tasks"})
    @Audit(action="AI_TASK", targetType="AI_TASK", detail="\u63d0\u4ea4\u4efb\u52a1: #{#body?.get('type')}")
    public BaseResponse<Map<String, Object>> submitTask(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        Map<String, Object> payload = new HashMap<String, Object>();
        String payloadJson = body.get("payload");
        if (payloadJson != null && !payloadJson.isBlank()) {
            try {
                payload = (Map)new ObjectMapper().readValue(payloadJson, (TypeReference)new TypeReference<Map<String, Object>>(){});
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        Integer priority = body.get("priority") == null ? null : Integer.valueOf(Integer.parseInt(body.get("priority")));
        AiTaskEntity task = this.aiTaskService.submit(body.get("type"), payload, priority, user.getUserId(), user.getTenantId());
        return BaseResponse.success(Map.of("taskId", task.getId(), "status", task.getStatus()));
    }

    @GetMapping(value={"/tasks/{id}"})
    public BaseResponse<Map<String, Object>> taskDetail(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        AiTaskEntity task = this.aiTaskService.detail(id, user.getTenantId());
        return BaseResponse.success(Map.of("taskId", task.getId(), "type", task.getType(), "status", task.getStatus(), "resultRef", task.getResultRef() == null ? "" : task.getResultRef(), "errorMsg", task.getErrorMsg() == null ? "" : task.getErrorMsg(), "createTime", task.getCreateTime() == null ? "" : task.getCreateTime().toString(), "finishTime", task.getFinishTime() == null ? "" : task.getFinishTime().toString()));
    }

    @GetMapping(value={"/tasks"})
    public BaseResponse<List<AiTaskEntity>> taskList(@RequestParam(required=false) String status, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.aiTaskService.list(user.getTenantId(), status));
    }

    @GetMapping(value={"/reports"})
    public BaseResponse<List<AiReportEntity>> reportList(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.reportComposeService.list(user.getTenantId()));
    }

    @GetMapping(value={"/reports/{id}"})
    public BaseResponse<AiReportEntity> reportDetail(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.reportComposeService.detail(id, user.getTenantId()));
    }

    @PostMapping(value={"/rules/parse"})
    public BaseResponse<Map<String, Object>> ruleParse(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2RuleService.parse(body.getOrDefault("text", ""), user));
    }

    @PostMapping(value={"/rules"})
    @Audit(action="CREATE", targetType="AI_RULE", detail="\u521b\u5efa\u9884\u8b66\u89c4\u5219: #{#body?.get('nluText')}")
    public BaseResponse<AiRuleEntity> ruleCreate(@RequestBody Map<String, Object> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2RuleService.create(String.valueOf(body.get("nluText")), (Map)body.get("ruleJson"), user));
    }

    @GetMapping(value={"/rules"})
    public BaseResponse<List<AiRuleEntity>> ruleList(@RequestParam(required=false) String status, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2RuleService.list(user.getTenantId(), status));
    }

    @PostMapping(value={"/rules/{id}/enable"})
    public BaseResponse<AiRuleEntity> ruleEnable(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2RuleService.setStatus(id, user.getTenantId(), "ENABLED"));
    }

    @PostMapping(value={"/rules/{id}/disable"})
    public BaseResponse<AiRuleEntity> ruleDisable(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2RuleService.setStatus(id, user.getTenantId(), "DISABLED"));
    }

    @DeleteMapping(value={"/rules/{id}"})
    public BaseResponse<Void> ruleDelete(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        this.nl2RuleService.delete(id, user.getTenantId());
        return BaseResponse.success();
    }

    @PostMapping(value={"/rules/evaluate"})
    public BaseResponse<Map<String, Object>> ruleEvaluate(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.nl2RuleService.evaluate(body, user));
    }

    @GetMapping(value={"/traces"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<List<AiTraceEntity>> traceList(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.aiTraceService.list(user.getTenantId()));
    }

    @GetMapping(value={"/traces/session/{sessionId}"})
    @PreAuthorize(value="hasRole('ADMIN')")
    public BaseResponse<List<AiTraceEntity>> traceBySession(@PathVariable String sessionId, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.aiTraceService.bySession(sessionId, user.getTenantId()));
    }

    @GetMapping(value={"/fraud"})
    public BaseResponse<Map<String, Object>> fraud(@RequestParam String keyword, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.fraudAnalysisService.analyze(keyword, user));
    }

    @PostMapping(value={"/sentiment"})
    public BaseResponse<Map<String, Object>> sentiment(@RequestBody Map<String, String> body) {
        return BaseResponse.success(this.sentimentAnalysisService.analyze(body.getOrDefault("text", "")));
    }

    @GetMapping(value={"/docs/templates"})
    public BaseResponse<Map<String, String>> docTemplates() {
        return BaseResponse.success(this.docTemplateService.templates());
    }

    @PostMapping(value={"/docs/generate"})
    public BaseResponse<Map<String, Object>> docGenerate(@RequestBody Map<String, Object> body) {
        return BaseResponse.success(this.docTemplateService.generate(String.valueOf(body.get("template")), (Map)body.get("variables")));
    }

    @GetMapping(value={"/plugins"})
    public BaseResponse<List<AiToolPluginEntity>> pluginList(@RequestParam(required=false) String status, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.pluginService.list(user.getTenantId(), status));
    }

    @PostMapping(value={"/plugins"})
    @Audit(action="CREATE", targetType="AI_PLUGIN", targetName="#{#{#body?.get('name')}}", detail="\u521b\u5efa\u5de5\u5177\u63d2\u4ef6")
    public BaseResponse<AiToolPluginEntity> pluginCreate(@RequestBody Map<String, Object> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.pluginService.create(String.valueOf(body.get("name")), String.valueOf(body.getOrDefault("description", "")), (List)body.get("chain"), user.getTenantId(), user.getUserId()));
    }

    @PostMapping(value={"/plugins/{id}/enable"})
    public BaseResponse<AiToolPluginEntity> pluginEnable(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.pluginService.setStatus(id, user.getTenantId(), "ENABLED"));
    }

    @PostMapping(value={"/plugins/{id}/disable"})
    public BaseResponse<AiToolPluginEntity> pluginDisable(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.pluginService.setStatus(id, user.getTenantId(), "DISABLED"));
    }

    @DeleteMapping(value={"/plugins/{id}"})
    public BaseResponse<Void> pluginDelete(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        this.pluginService.delete(id, user.getTenantId());
        return BaseResponse.success();
    }

    @PostMapping(value={"/plugins/{id}/run"})
    @Audit(action="AI_TOOL", targetType="AI_PLUGIN", targetName="#{#{#id}}", detail="\u6267\u884c\u5de5\u5177\u63d2\u4ef6")
    public BaseResponse<Map<String, Object>> pluginRun(@PathVariable String id, @RequestBody(required=false) Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.pluginService.run(id, user.getTenantId(), user, body == null ? "" : body.getOrDefault("keyword", "")));
    }

    @GetMapping(value={"/memory"})
    public BaseResponse<List<Map<String, String>>> memoryList(@RequestParam(required=false, defaultValue="USER_PREF") String type, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.memoryService.recall(user.getUserId(), user.getTenantId(), type).stream().map(m -> Map.of("key", m.getKey(), "type", m.getType(), "content", m.getContent() == null ? "" : m.getContent())).collect(Collectors.toList()));
    }

    @PostMapping(value={"/memory"})
    public BaseResponse<Void> memoryRemember(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        this.memoryService.remember(user.getUserId(), user.getTenantId(), body.getOrDefault("type", "USER_PREF"), body.get("key"), body.get("content"), body.get("ttlDays") == null ? null : Integer.valueOf(Integer.parseInt(body.get("ttlDays"))));
        return BaseResponse.success();
    }

    @DeleteMapping(value={"/memory/{key}"})
    public BaseResponse<Void> memoryForget(@PathVariable String key, @AuthenticationPrincipal LoginUser user) {
        this.memoryService.forget(user.getUserId(), user.getTenantId(), key);
        return BaseResponse.success();
    }

    @GetMapping(value={"/tools"})
    public BaseResponse<Map<String, String>> tools() {
        return BaseResponse.success(this.toolRegistry.all().values().stream().collect(Collectors.toMap(ToolRegistry.Tool::name, ToolRegistry.Tool::description)));
    }

    @GetMapping(value={"/anomaly/clues"})
    public BaseResponse<List<AnomalyClueEntity>> clues(@RequestParam(required=false) String status, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.anomalyDetector.list(user.getTenantId(), status));
    }

    @PostMapping(value={"/anomaly/detect"})
    @Audit(action="AI_ANALYZE", targetType="ANOMALY_DETECT", detail="\u89e6\u53d1\u5f02\u5e38\u68c0\u6d4b")
    public BaseResponse<Void> detect(@AuthenticationPrincipal LoginUser user) {
        this.anomalyDetector.detect(user.getTenantId());
        return BaseResponse.success();
    }

    @PostMapping(value={"/anomaly/clues/{id}/confirm"})
    @Audit(action="UPDATE", targetType="ANOMALY_CLUE", targetName="#{#{#id}}", detail="\u786e\u8ba4\u7ebf\u7d22")
    public BaseResponse<AnomalyClueEntity> confirm(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.anomalyDetector.confirm(id, user.getTenantId()));
    }

    @PostMapping(value={"/anomaly/clues/{id}/dismiss"})
    @Audit(action="UPDATE", targetType="ANOMALY_CLUE", targetName="#{#{#id}}", detail="\u9a73\u56de\u7ebf\u7d22")
    public BaseResponse<AnomalyClueEntity> dismiss(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.anomalyDetector.dismiss(id, user.getTenantId()));
    }

    public AiAnalysisController(AnalysisAgent analysisAgent, ToolRegistry toolRegistry, Nl2SqlService nl2SqlService, RagService ragService, AnomalyDetector anomalyDetector, AiTaskService aiTaskService, MemoryService memoryService, SemanticCacheService semanticCacheService, PluginService pluginService, ReportComposeService reportComposeService, Nl2RuleService nl2RuleService, AiTraceService aiTraceService, FraudAnalysisService fraudAnalysisService, SentimentAnalysisService sentimentAnalysisService, DocTemplateService docTemplateService) {
        this.analysisAgent = analysisAgent;
        this.toolRegistry = toolRegistry;
        this.nl2SqlService = nl2SqlService;
        this.ragService = ragService;
        this.anomalyDetector = anomalyDetector;
        this.aiTaskService = aiTaskService;
        this.memoryService = memoryService;
        this.semanticCacheService = semanticCacheService;
        this.pluginService = pluginService;
        this.reportComposeService = reportComposeService;
        this.nl2RuleService = nl2RuleService;
        this.aiTraceService = aiTraceService;
        this.fraudAnalysisService = fraudAnalysisService;
        this.sentimentAnalysisService = sentimentAnalysisService;
        this.docTemplateService = docTemplateService;
    }
}

