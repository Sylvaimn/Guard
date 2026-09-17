package com.temporedata.guard.modules.police.network.controller;
import com.temporedata.guard.modules.police.network.service.ContactAnalysisService;
import com.temporedata.guard.modules.police.network.service.FundFlowService;
import com.temporedata.guard.modules.police.network.entity.GraphEdgeEntity;
import com.temporedata.guard.modules.police.network.service.GraphNetworkService;
import com.temporedata.guard.modules.police.network.entity.RiskScoreEntity;
import com.temporedata.guard.modules.police.network.service.RiskScoreService;
import com.temporedata.guard.modules.police.network.service.SimilarityService;
import com.temporedata.guard.modules.police.network.service.SpatialTemporalService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.security.security.audit.Audit;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 关系网络 + 资金流 + 通联 API（P1）。
 */
@RestController
@RequestMapping("/api/network")
@RequiredArgsConstructor
public class NetworkController {

    private final GraphNetworkService networkService;
    private final FundFlowService fundFlowService;
    private final ContactAnalysisService contactAnalysisService;
    private final SimilarityService similarityService;
    private final SpatialTemporalService spatialTemporalService;
    private final com.temporedata.guard.modules.police.network.service.RiskScoreService riskScoreService;

    // ---- 统一边 ----

    @PostMapping("/edges")
    @Audit(action = "CREATE", targetType = "GRAPH_EDGE", detail = "录入关系边 #{#body?.get('relationType')}")
    public BaseResponse<GraphEdgeEntity> addEdge(@RequestBody Map<String, Object> body,
                                                 @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(networkService.addEdge(
                String.valueOf(body.get("sourceEntityId")), String.valueOf(body.get("targetEntityId")),
                String.valueOf(body.get("relationType")),
                body.get("weight") == null ? null : Double.valueOf(String.valueOf(body.get("weight"))),
                String.valueOf(body.getOrDefault("evidence", "")), user.getTenantId()));
    }

    @GetMapping("/{entityId}/edges")
    public BaseResponse<List<GraphEdgeEntity>> edgesOf(@PathVariable String entityId,
                                                       @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(networkService.edgesOf(entityId, user.getTenantId()));
    }

    @GetMapping("/{entityId}/trace")
    public BaseResponse<List<Map<String, Object>>> trace(@PathVariable String entityId,
                                                         @RequestParam(required = false) String type,
                                                         @RequestParam(defaultValue = "3") int depth,
                                                         @RequestParam(required = false) Double minWeight,
                                                         @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(networkService.trace(entityId, type, depth, minWeight, user.getTenantId()));
    }

    @GetMapping("/communities")
    public BaseResponse<List<Map<String, Object>>> communities(@RequestParam String type,
                                                               @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(networkService.communities(type, user.getTenantId()));
    }

    @GetMapping("/{entityId}/centrality")
    public BaseResponse<Map<String, Object>> centrality(@PathVariable String entityId,
                                                        @RequestParam(required = false) String type,
                                                        @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(networkService.centrality(entityId, type, user.getTenantId()));
    }

    // ---- 资金流 ----

    @PostMapping("/fund/edges")
    @Audit(action = "CREATE", targetType = "FUND_EDGE", detail = "录入资金边 #{#body?.get('fromAccount')} → #{#body?.get('toAccount')}")
    public BaseResponse<GraphEdgeEntity> addFundEdge(@RequestBody Map<String, Object> body,
                                                     @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(fundFlowService.addFundEdge(
                String.valueOf(body.get("fromAccount")), String.valueOf(body.get("toAccount")),
                body.get("amount") == null ? null : Double.valueOf(String.valueOf(body.get("amount"))),
                String.valueOf(body.getOrDefault("evidence", "")), user));
    }

    @GetMapping("/fund/{accountId}/trace")
    public BaseResponse<List<Map<String, Object>>> fundTrace(@PathVariable String accountId,
                                                             @RequestParam(defaultValue = "3") int depth,
                                                             @RequestParam(required = false) Double minAmount,
                                                             @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(fundFlowService.trace(accountId, depth, minAmount, user));
    }

    @GetMapping("/fund/inflow")
    public BaseResponse<List<Map<String, Object>>> fundInflow(@RequestParam(defaultValue = "10") int topN,
                                                              @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(fundFlowService.inflowAggregate(user.getTenantId(), topN));
    }

    // ---- 串并案(相似引擎) ----

    @GetMapping("/similar-cases")
    public BaseResponse<Map<String, Object>> similarCases(@RequestParam String keyword,
                                                          @RequestParam(defaultValue = "false") boolean persistEdge,
                                                          @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(similarityService.similarCases(keyword, user, persistEdge));
    }

    // ---- 时空分析 ----

    @GetMapping("/st/hotspots")
    public BaseResponse<List<Map<String, Object>>> hotSpots(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @RequestParam(defaultValue = "10") int topN, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(spatialTemporalService.hotSpots(user.getTenantId(), from, to, topN));
    }

    @GetMapping("/st/overview")
    public BaseResponse<Map<String, Object>> stOverview(
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime from,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.LocalDateTime to,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(spatialTemporalService.overview(user.getTenantId(), from, to));
    }

    // ---- 重点人员管控积分 ----

    @PostMapping("/risk/{entityId}/evaluate")
    @Audit(action = "EXECUTE", targetType = "RISK_SCORE", targetName = "#{#{#entityId}}", detail = "风险积分评估")
    public BaseResponse<com.temporedata.guard.modules.police.network.entity.RiskScoreEntity> riskEvaluate(
            @PathVariable String entityId, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(riskScoreService.evaluate(entityId, user.getTenantId()));
    }

    @GetMapping("/risk")
    public BaseResponse<List<com.temporedata.guard.modules.police.network.entity.RiskScoreEntity>> riskList(
            @RequestParam(required = false) String level, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(riskScoreService.list(user.getTenantId(), level));
    }

    @GetMapping("/risk/{entityId}")
    public BaseResponse<com.temporedata.guard.modules.police.network.entity.RiskScoreEntity> riskDetail(
            @PathVariable String entityId, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(riskScoreService.detail(entityId, user.getTenantId()));
    }

    // ---- 通联 ----

    @PostMapping("/contact/edges")
    @Audit(action = "CREATE", targetType = "CONTACT_EDGE", detail = "录入通联边 #{#body?.get('fromEntity')} → #{#body?.get('toEntity')}")
    public BaseResponse<GraphEdgeEntity> addContactEdge(@RequestBody Map<String, Object> body,
                                                        @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(contactAnalysisService.addContactEdge(
                String.valueOf(body.get("fromEntity")), String.valueOf(body.get("toEntity")),
                body.get("frequency") == null ? null : Double.valueOf(String.valueOf(body.get("frequency"))),
                String.valueOf(body.getOrDefault("evidence", "")), user));
    }

    @GetMapping("/contact/frequent")
    public BaseResponse<List<Map<String, Object>>> frequentContacts(@RequestParam(defaultValue = "10") int topN,
                                                                    @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(contactAnalysisService.frequentContacts(user.getTenantId(), topN));
    }

    @GetMapping("/contact/gangs")
    public BaseResponse<List<Map<String, Object>>> contactGangs(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(contactAnalysisService.gangs(user.getTenantId()));
    }

    @GetMapping("/contact/key-persons")
    public BaseResponse<List<Map<String, Object>>> keyPersons(@RequestParam(defaultValue = "10") int topN,
                                                              @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(contactAnalysisService.keyPersons(user.getTenantId(), topN));
    }
}
