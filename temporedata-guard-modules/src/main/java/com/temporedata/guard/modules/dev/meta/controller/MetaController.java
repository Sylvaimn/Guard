package com.temporedata.guard.modules.dev.meta.controller;
import com.temporedata.guard.modules.dev.meta.service.MetaLineageService;
import com.temporedata.guard.modules.dev.meta.service.MetaService;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.meta.*;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;
    private final MetaLineageService metaLineageService;

    // ==================== Metadata Collection ====================

    @PostMapping("/sync/{datasourceId}")
    public BaseResponse<Map<String, Object>> syncMetadata(
            @PathVariable String datasourceId,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaService.syncMetadata(datasourceId, user.getTenantId()));
    }

    @PostMapping("/sync-all")
    public BaseResponse<Map<String, Object>> syncAllMetadata(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaService.syncAllMetadata(user.getTenantId()));
    }

    @GetMapping("/overview")
    public BaseResponse<Map<String, Object>> overview(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaService.getOverview(user.getTenantId()));
    }

    // ==================== Table Query ====================

    @GetMapping("/tables")
    public BaseResponse<List<MetaTableRes>> listTables(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaService.listTables(user.getTenantId()));
    }

    @GetMapping("/tables/datasource/{datasourceId}")
    public BaseResponse<List<MetaTableRes>> listTablesByDatasource(@PathVariable String datasourceId) {
        return BaseResponse.success(metaService.listTablesByDatasource(datasourceId));
    }

    @GetMapping("/tables/{tableId}")
    public BaseResponse<MetaTableRes> getTableDetail(@PathVariable String tableId) {
        return BaseResponse.success(metaService.getTableDetail(tableId));
    }

    @GetMapping("/tables/{tableId}/columns")
    public BaseResponse<List<MetaColumnRes>> getColumns(@PathVariable String tableId) {
        return BaseResponse.success(metaService.getColumns(tableId));
    }

    // ==================== Lineage ====================

    @PostMapping("/lineage/build/{datasourceId}")
    public BaseResponse<Void> buildLineage(
            @PathVariable String datasourceId,
            @AuthenticationPrincipal LoginUser user) {
        metaService.buildLineage(datasourceId, user.getTenantId());
        return BaseResponse.success();
    }

    @GetMapping("/lineage/{tableId}")
    public BaseResponse<MetaLineageRes> getLineage(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "3") int maxDepth) {
        return BaseResponse.success(metaService.getLineage(tableId, maxDepth));
    }

    // ==================== Enhanced Lineage Analysis ====================

    @GetMapping("/lineage/graph/{tableId}")
    public BaseResponse<LineageGraphRes> getLineageGraph(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "4") int maxDepth) {
        return BaseResponse.success(metaLineageService.buildLineageGraph(tableId, maxDepth));
    }

    @GetMapping("/lineage/impact/{tableId}")
    public BaseResponse<ImpactAnalysisRes> getImpactAnalysis(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "5") int maxDepth) {
        return BaseResponse.success(metaLineageService.analyzeImpact(tableId, maxDepth));
    }

    @GetMapping("/lineage/field/{tableId}/{columnName}")
    public BaseResponse<List<LineageGraphRes.FieldLink>> getFieldLineage(
            @PathVariable String tableId,
            @PathVariable String columnName) {
        return BaseResponse.success(metaLineageService.getFieldLineage(tableId, columnName));
    }

    @GetMapping("/lineage/search")
    public BaseResponse<List<MetaTableRes>> searchLineage(
            @RequestParam String keyword,
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaLineageService.searchLineage(keyword, user.getTenantId()));
    }

    @GetMapping("/lineage/relations")
    public BaseResponse<List<MetaLineageRes>> listAllLineage(
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaLineageService.listAllLineage(user.getTenantId()));
    }

    @PostMapping("/lineage/analyze-sql")
    public BaseResponse<SqlLineageRes> analyzeSql(@RequestBody Map<String, String> body,
                                                  @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(metaLineageService.analyzeSql(body.get("sql"), user.getTenantId()));
    }

    @GetMapping("/lineage/analyze/{tableId}")
    public BaseResponse<LineageAnalysisRes> analyzeGraph(
            @PathVariable String tableId,
            @RequestParam(defaultValue = "4") int maxDepth) {
        return BaseResponse.success(metaLineageService.analyzeGraph(tableId, maxDepth));
    }

    @PostMapping("/lineage/add")
    public BaseResponse<Void> addLineage(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal LoginUser user) {
        metaLineageService.addLineage(
                (String) body.get("sourceTableId"),
                (String) body.get("targetTableId"),
                (String) body.get("relationType"),
                (String) body.get("relationName"),
                (String) body.get("transformationSql"),
                (String) body.get("description"),
                user.getTenantId());
        return BaseResponse.success();
    }

    @PostMapping("/lineage/field/add")
    public BaseResponse<Void> addFieldLineage(@RequestBody Map<String, Object> body) {
        metaLineageService.addFieldLineage(
                (String) body.get("lineageId"),
                (String) body.get("sourceColumn"),
                (String) body.get("targetColumn"),
                (String) body.get("transformationLogic"),
                (String) body.get("comment"));
        return BaseResponse.success();
    }

    @DeleteMapping("/lineage/{lineageId}")
    public BaseResponse<Void> deleteLineage(@PathVariable String lineageId) {
        metaLineageService.deleteLineage(lineageId);
        return BaseResponse.success();
    }
}