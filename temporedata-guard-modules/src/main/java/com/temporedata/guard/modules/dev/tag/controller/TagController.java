package com.temporedata.guard.modules.dev.tag.controller;
import com.temporedata.guard.modules.dev.tag.entity.ClassificationEntity;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.tag.TagBindingReq;
import com.temporedata.guard.api.tag.TagBindingRes;
import com.temporedata.guard.api.tag.TagReq;
import com.temporedata.guard.api.tag.TagRes;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.dev.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Data tag REST API.
 */
@RestController
@RequestMapping("/api/tag")
@RequiredArgsConstructor
public class TagController {

    private final TagService tagService;

    @GetMapping("/list")
    public BaseResponse<List<TagRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(tagService.listAll(user.getTenantId()));
    }

    @PostMapping("/create")
    public BaseResponse<TagRes> create(@RequestBody TagReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(tagService.create(req, user.getTenantId()));
    }

    @PutMapping("/{id}")
    public BaseResponse<TagRes> update(@PathVariable String id, @RequestBody TagReq req) {
        return BaseResponse.success(tagService.update(id, req));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        tagService.delete(id);
        return BaseResponse.success();
    }

    // ==================== Approval workflow ====================

    @PostMapping("/{id}/submit")
    public BaseResponse<TagRes> submit(@PathVariable String id) {
        return BaseResponse.success(tagService.submit(id));
    }

    @PostMapping("/{id}/approve")
    public BaseResponse<TagRes> approve(@PathVariable String id) {
        return BaseResponse.success(tagService.approve(id));
    }

    @PostMapping("/{id}/reject")
    public BaseResponse<TagRes> reject(@PathVariable String id) {
        return BaseResponse.success(tagService.reject(id));
    }

    // ==================== Classification ====================

    @GetMapping("/classification/list")
    public BaseResponse<List<com.temporedata.guard.modules.dev.tag.entity.ClassificationEntity>> listClassifications(
            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(tagService.listClassifications(user.getTenantId()));
    }

    @PostMapping("/classification/create")
    public BaseResponse<com.temporedata.guard.modules.dev.tag.entity.ClassificationEntity> createClassification(
            @RequestParam String name, @RequestParam(required = false) String code,
            @RequestParam(required = false) String description, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(tagService.createClassification(name, code, description, user.getTenantId()));
    }

    @DeleteMapping("/classification/{id}")
    public BaseResponse<Void> deleteClassification(@PathVariable String id) {
        tagService.deleteClassification(id);
        return BaseResponse.success();
    }

    // ==================== Asset Binding ====================

    @PostMapping("/bind")
    public BaseResponse<Void> bind(@RequestBody TagBindingReq req, @AuthenticationPrincipal LoginUser user) {
        tagService.bind(req, user.getTenantId());
        return BaseResponse.success();
    }

    @DeleteMapping("/unbind")
    public BaseResponse<Void> unbind(@RequestParam String tagId, @RequestParam String assetType, @RequestParam String assetId) {
        tagService.unbind(tagId, assetType, assetId);
        return BaseResponse.success();
    }

    @GetMapping("/bindings")
    public BaseResponse<List<TagBindingRes>> listByAsset(@RequestParam String assetType, @RequestParam String assetId) {
        return BaseResponse.success(tagService.listByAsset(assetType, assetId));
    }

    @GetMapping("/bindings/all")
    public BaseResponse<List<TagBindingRes>> listAllBindings(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(tagService.listAllBindings(user.getTenantId()));
    }
}