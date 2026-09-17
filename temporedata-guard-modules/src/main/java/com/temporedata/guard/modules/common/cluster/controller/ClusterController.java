package com.temporedata.guard.modules.common.cluster.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.cluster.ClusterNodeReq;
import com.temporedata.guard.api.cluster.ClusterNodeRes;
import com.temporedata.guard.api.cluster.ClusterReq;
import com.temporedata.guard.api.cluster.ClusterRes;
import com.temporedata.guard.modules.common.cluster.service.ClusterService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Cluster management REST API.
 */
@RestController
@RequestMapping("/api/cluster")
@RequiredArgsConstructor
public class ClusterController {

    private final ClusterService clusterService;

    // ===== Cluster CRUD =====

    @GetMapping
    public BaseResponse<List<ClusterRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(clusterService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<ClusterRes> detail(@PathVariable String id) {
        return BaseResponse.success(clusterService.detail(id));
    }

    @PostMapping
    public BaseResponse<ClusterRes> create(@RequestBody ClusterReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(clusterService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<ClusterRes> update(@PathVariable String id, @RequestBody ClusterReq req) {
        return BaseResponse.success(clusterService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        clusterService.delete(id);
        return BaseResponse.success();
    }

    // ===== Cluster Node CRUD =====

    @GetMapping("/{id}/nodes")
    public BaseResponse<List<ClusterNodeRes>> listNodes(@PathVariable String id) {
        return BaseResponse.success(clusterService.listNodes(id));
    }

    @PostMapping("/{id}/nodes")
    public BaseResponse<ClusterNodeRes> addNode(@PathVariable String id, @RequestBody ClusterNodeReq req) {
        return BaseResponse.success(clusterService.addNode(id, req));
    }

    // ===== Agent Operations =====

    @PostMapping("/nodes/{nodeId}/install")
    public BaseResponse<String> installAgent(@PathVariable String nodeId) {
        return BaseResponse.success(clusterService.installAgent(nodeId));
    }

    @GetMapping("/nodes/{nodeId}/status")
    public BaseResponse<ClusterNodeRes> checkNodeStatus(@PathVariable String nodeId) {
        return BaseResponse.success(clusterService.checkNodeStatus(nodeId));
    }
}