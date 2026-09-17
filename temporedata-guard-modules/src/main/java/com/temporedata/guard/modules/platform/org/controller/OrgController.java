package com.temporedata.guard.modules.platform.org.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.org.OrgMemberRes;
import com.temporedata.guard.modules.platform.org.entity.OrgEntity;
import com.temporedata.guard.modules.platform.org.service.OrgService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/org")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    @GetMapping("/tree")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> tree(@AuthenticationPrincipal LoginUser user) {
        return orgService.tree(user.getTenantId());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<OrgEntity> list(@AuthenticationPrincipal LoginUser user) {
        return orgService.listByTenant(user.getTenantId());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public OrgEntity create(@RequestBody Map<String, String> body, @AuthenticationPrincipal LoginUser user) {
        return orgService.createOrg(
                body.get("name"), body.get("parentId"),
                user.getTenantId(), body.get("remark"),
                body.get("defaultRoleId"),
                body.get("orgCode"), body.get("policeType"),
                body.get("headcount") == null ? null : Integer.valueOf(body.get("headcount")),
                body.get("orgType"), body.get("expireTime"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public OrgEntity update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return orgService.updateOrg(id, body.get("name"), body.get("parentId"),
                body.get("remark"), body.get("responsibleUserId"), body.get("defaultRoleId"),
                body.get("orgCode"), body.get("policeType"),
                body.get("headcount") == null ? null : Integer.valueOf(body.get("headcount")),
                body.get("orgType"), body.get("expireTime"));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        orgService.deleteOrg(id);
        return BaseResponse.success();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/members")
    public BaseResponse<List<OrgMemberRes>> members(@PathVariable String id) {
        return BaseResponse.success(orgService.members(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/members/{userId}")
    public BaseResponse<Void> assignMember(@PathVariable String id, @PathVariable String userId) {
        orgService.assignMember(id, userId);
        return BaseResponse.success();
    }

    @DeleteMapping("/{id}/members/{userId}")
    public BaseResponse<Void> removeMember(@PathVariable String id, @PathVariable String userId) {
        orgService.removeMember(id, userId);
        return BaseResponse.success();
    }

    @PutMapping("/{id}/responsible/{userId}")
    public BaseResponse<Void> setResponsible(@PathVariable String id, @PathVariable String userId) {
        orgService.updateOrg(id, null, null, null, userId, null, null, null, null, null, null);
        return BaseResponse.success();
    }

    @PutMapping("/transfer/{userId}")
    public BaseResponse<Void> transferMember(@PathVariable String userId, @RequestParam String newOrgId) {
        orgService.transferMember(userId, newOrgId);
        return BaseResponse.success();
    }
}
