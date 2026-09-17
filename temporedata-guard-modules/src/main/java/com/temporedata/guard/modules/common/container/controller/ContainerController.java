package com.temporedata.guard.modules.common.container.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.common.container.entity.ContainerEntity;
import com.temporedata.guard.modules.common.container.service.ContainerService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/container")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    @GetMapping
    public BaseResponse<List<ContainerEntity>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(containerService.listContainers(user.getTenantId()));
    }

    @GetMapping("/cluster/{clusterId}")
    public BaseResponse<List<ContainerEntity>> listByCluster(@PathVariable String clusterId) {
        return BaseResponse.success(containerService.listByCluster(clusterId));
    }

    @GetMapping("/{id}")
    public BaseResponse<ContainerEntity> get(@PathVariable String id) {
        return BaseResponse.success(containerService.getContainer(id));
    }

    @PostMapping
    public BaseResponse<ContainerEntity> create(@RequestBody ContainerEntity container) {
        return BaseResponse.success(containerService.createContainer(container));
    }

    @PutMapping("/{id}")
    public BaseResponse<ContainerEntity> update(@PathVariable String id, @RequestBody ContainerEntity container) {
        return BaseResponse.success(containerService.updateContainer(id, container));
    }

    @PostMapping("/{id}/start")
    public BaseResponse<ContainerEntity> start(@PathVariable String id) {
        return BaseResponse.success(containerService.startContainer(id));
    }

    @PostMapping("/{id}/stop")
    public BaseResponse<ContainerEntity> stop(@PathVariable String id) {
        return BaseResponse.success(containerService.stopContainer(id));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        containerService.deleteContainer(id);
        return BaseResponse.success();
    }
}