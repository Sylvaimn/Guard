package com.temporedata.guard.modules.dev.catalog.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.catalog.*;
import com.temporedata.guard.modules.dev.catalog.service.CatalogService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/catalog")
@RequiredArgsConstructor
public class CatalogController {

    private final CatalogService catalogService;

    @GetMapping
    public BaseResponse<List<TableMetadataRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(catalogService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<TableMetadataRes> detail(@PathVariable String id) {
        return BaseResponse.success(catalogService.detail(id));
    }

    @PostMapping("/sync/{datasourceId}")
    public BaseResponse<Void> sync(@PathVariable String datasourceId) {
        catalogService.sync(datasourceId);
        return BaseResponse.success();
    }

    @PutMapping("/comment")
    public BaseResponse<Void> updateComment(@RequestBody CommentUpdateReq req) {
        catalogService.updateComment(req);
        return BaseResponse.success();
    }

    // ===== P4: Enhanced catalog endpoints =====

    @PostMapping("/sync-all")
    public BaseResponse<Map<String, Object>> syncAll() {
        return BaseResponse.success(catalogService.syncAll());
    }

    @GetMapping("/tree")
    public BaseResponse<List<CatalogTreeRes>> tree(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(catalogService.tree(user.getTenantId()));
    }

    @GetMapping("/lineage/{tableId}")
    public BaseResponse<LineageRes> lineage(@PathVariable String tableId) {
        return BaseResponse.success(catalogService.lineage(tableId));
    }

    @PutMapping("/governance")
    public BaseResponse<Void> updateGovernance(@RequestBody GovernanceUpdateReq req) {
        catalogService.updateGovernance(req);
        return BaseResponse.success();
    }
}