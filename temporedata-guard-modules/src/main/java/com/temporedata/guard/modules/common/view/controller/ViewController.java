package com.temporedata.guard.modules.common.view.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.common.view.entity.ViewEntity;
import com.temporedata.guard.modules.common.view.service.ViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/view")
@RequiredArgsConstructor
public class ViewController {

    private final ViewService viewService;

    @GetMapping
    public BaseResponse<List<ViewEntity>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(viewService.listViews(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<ViewEntity> get(@PathVariable String id) {
        return BaseResponse.success(viewService.getView(id));
    }

    @PostMapping
    public BaseResponse<ViewEntity> create(@RequestBody ViewEntity view) {
        return BaseResponse.success(viewService.createView(view));
    }

    @PutMapping("/{id}")
    public BaseResponse<ViewEntity> update(@PathVariable String id, @RequestBody ViewEntity view) {
        return BaseResponse.success(viewService.updateView(id, view));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        viewService.deleteView(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/publish")
    public BaseResponse<ViewEntity> publish(@PathVariable String id) {
        return BaseResponse.success(viewService.publishView(id));
    }

    @PostMapping("/{id}/execute")
    public BaseResponse<Map<String, Object>> execute(@PathVariable String id) {
        return BaseResponse.success(viewService.executeView(id));
    }
}