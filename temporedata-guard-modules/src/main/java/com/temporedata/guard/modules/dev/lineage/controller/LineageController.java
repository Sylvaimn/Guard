package com.temporedata.guard.modules.dev.lineage.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.lineage.CycleAnalysisRes;
import com.temporedata.guard.api.lineage.HotNodeRes;
import com.temporedata.guard.api.lineage.LineagePathRes;
import com.temporedata.guard.api.meta.ImpactAnalysisRes;
import com.temporedata.guard.api.meta.LineageGraphRes;
import com.temporedata.guard.modules.dev.lineage.service.LineageService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Enterprise lineage REST API:
 * /api/lineage/graph, /path, /cycles, /impact, /lineage(trace), /search, /heat, /rebuild, /export.
 */
@RestController
@RequestMapping("/api/lineage")
@RequiredArgsConstructor
public class LineageController {

    private final LineageService lineageService;

    @PostMapping("/rebuild")
    public BaseResponse<Map<String, Object>> rebuild(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.rebuild(user.getTenantId()));
    }

    @GetMapping("/graph")
    public BaseResponse<LineageGraphRes> graph(
            @RequestParam String nodeId,
            @RequestParam(defaultValue = "both") String direction,
            @RequestParam(defaultValue = "3") int depth,
            @RequestParam(defaultValue = "table") String level,
            @RequestParam(required = false) List<String> nodeTypes,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.getGraph(nodeId, direction, depth, level, nodeTypes, user.getTenantId()));
    }

    @GetMapping("/field")
    public BaseResponse<LineageGraphRes> field(
            @RequestParam String tableId,
            @RequestParam String columnName,
            @RequestParam(defaultValue = "both") String direction,
            @RequestParam(defaultValue = "3") int depth,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.fieldLineage(tableId, columnName, direction, depth, user.getTenantId()));
    }

    @GetMapping("/overview")
    public BaseResponse<LineageGraphRes> overview(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.overview(user.getTenantId()));
    }

    @GetMapping("/impact")
    public BaseResponse<ImpactAnalysisRes> impact(
            @RequestParam String nodeId,
            @RequestParam(defaultValue = "5") int depth,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.impact(nodeId, depth, user.getTenantId()));
    }

    @GetMapping("/lineage")
    public BaseResponse<ImpactAnalysisRes> lineageTrace(
            @RequestParam String nodeId,
            @RequestParam(defaultValue = "5") int depth,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.impact(nodeId, depth, user.getTenantId()));
    }

    @PostMapping("/path")
    public BaseResponse<LineagePathRes> path(@RequestBody Map<String, String> body,
                                             @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.findPath(
                body.get("sourceId"), body.get("targetId"), user.getTenantId()));
    }

    @GetMapping("/cycles")
    public BaseResponse<CycleAnalysisRes> cycles(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.findCycles(user.getTenantId()));
    }

    @GetMapping("/heat")
    public BaseResponse<List<HotNodeRes>> heat(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.heat(user.getTenantId()));
    }

    @GetMapping("/search")
    public BaseResponse<List<Map<String, Object>>> search(
            @RequestParam String keyword,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.search(keyword, user.getTenantId()));
    }

    @GetMapping("/export")
    public BaseResponse<String> export(
            @RequestParam String nodeId,
            @RequestParam(defaultValue = "3") int depth,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.exportJson(nodeId, depth, user.getTenantId()));
    }

    @GetMapping("/dump")
    public BaseResponse<Map<String, Object>> dump(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(lineageService.dumpForDebug(user.getTenantId()));
    }
}