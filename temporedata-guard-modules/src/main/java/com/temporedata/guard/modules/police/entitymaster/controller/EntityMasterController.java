package com.temporedata.guard.modules.police.entitymaster.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.entitymaster.EntityLabelRes;
import com.temporedata.guard.api.entitymaster.EntityMasterRes;
import com.temporedata.guard.api.entitymaster.EntityRegisterReq;
import com.temporedata.guard.modules.police.entitymaster.service.EntityMasterService;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.security.security.audit.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/entitymaster")
@RequiredArgsConstructor
public class EntityMasterController {

    private final EntityMasterService entityMasterService;

    @GetMapping
    public BaseResponse<List<EntityMasterRes>> list(@RequestParam(required = false) String entityType,
                                                    @RequestParam(required = false) String status,
                                                    @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(entityMasterService.list(entityType, status, user.getTenantId(), user.getDensity()));
    }

    @GetMapping("/{id}")
    public BaseResponse<EntityMasterRes> detail(@PathVariable String id,
                                                @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(entityMasterService.detail(id, user.getDensity()));
    }

    @Audit(action = "DELETE", targetType = "ENTITY_MASTER", targetName = "#{#id}")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        entityMasterService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/register")
    @Audit(action = "CREATE", targetType = "ENTITY_MASTER", targetName = "#{#req.displayName}", detail = "注册实体 #{#req.entityType}")
    public BaseResponse<EntityMasterRes> register(@RequestBody EntityRegisterReq req,
                                                  @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(entityMasterService.register(req, user.getTenantId(), user.getDensity()));
    }

    @Audit(action = "UPDATE", targetType = "ENTITY_MASTER", targetName = "#{#id}", detail = "确认实体为MASTER")
    @PostMapping("/{id}/confirm")
    public BaseResponse<EntityMasterRes> confirm(@PathVariable String id,
                                                 @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(entityMasterService.confirm(id, user.getDensity()));
    }

    @Audit(action = "UPDATE", targetType = "ENTITY_MASTER", targetName = "#{#masterId}", detail = "合并候选实体 #{#candidateId}")
    @PostMapping("/merge")
    public BaseResponse<EntityMasterRes> merge(@RequestParam String masterId, @RequestParam String candidateId,
                                               @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(entityMasterService.merge(masterId, candidateId, user.getDensity()));
    }

    @Audit(action = "CREATE", targetType = "ENTITY_MASTER_LABEL", targetName = "#{#id}", detail = "#{#body.get('labelName')}")
    @PostMapping("/{id}/labels")
    public BaseResponse<EntityLabelRes> addLabel(@PathVariable String id, @RequestBody Map<String, String> body,
                                                 @AuthenticationPrincipal LoginUser user) {
        LocalDateTime expire = body.get("expireTime") != null && !body.get("expireTime").isBlank()
                ? LocalDateTime.parse(body.get("expireTime")) : null;
        return BaseResponse.success(entityMasterService.addLabel(
                id, body.get("labelType"), body.get("labelCode"), body.get("labelName"),
                body.get("source"),
                body.get("confidence") != null ? Double.parseDouble(body.get("confidence")) : null,
                expire, user.getTenantId()));
    }
}
