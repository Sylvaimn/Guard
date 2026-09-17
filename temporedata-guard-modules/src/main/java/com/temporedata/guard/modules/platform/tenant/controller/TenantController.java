package com.temporedata.guard.modules.platform.tenant.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.tenant.TenantMemberReq;
import com.temporedata.guard.api.tenant.TenantMemberRes;
import com.temporedata.guard.api.tenant.TenantReq;
import com.temporedata.guard.api.tenant.TenantRes;
import com.temporedata.guard.modules.platform.tenant.service.TenantService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Tenant management REST API.
 */
@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<List<TenantRes>> listAll() {
        return BaseResponse.success(tenantService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<TenantRes> create(@RequestBody TenantReq req) {
        return BaseResponse.success(tenantService.createTenant(
                req.getName(), req.getCode(), req.getDescription(),
                req.getContactName(), req.getContactEmail()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<TenantRes> update(@PathVariable String id, @RequestBody TenantReq req) {
        return BaseResponse.success(tenantService.updateTenant(
                id, req.getName(), req.getDescription(),
                req.getContactName(), req.getContactEmail()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> delete(@PathVariable String id) {
        tenantService.deleteTenant(id);
        return BaseResponse.success();
    }

    @GetMapping("/{tenantId}/members")
    public BaseResponse<List<TenantMemberRes>> listMembers(@PathVariable String tenantId) {
        return BaseResponse.success(tenantService.listMembers(tenantId));
    }

    @PostMapping("/{tenantId}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<TenantMemberRes> addMember(@PathVariable String tenantId, @RequestBody TenantMemberReq req) {
        return BaseResponse.success(tenantService.addMember(
                tenantId, req.getUsername(), req.getPassword(), req.getRoleCode(), req.getOrgId()));
    }

    @DeleteMapping("/{tenantId}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Void> removeMember(@PathVariable String tenantId, @PathVariable String userId) {
        tenantService.removeMember(tenantId, userId);
        return BaseResponse.success();
    }

    @PutMapping("/{tenantId}/members/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<TenantMemberRes> setMemberStatus(@PathVariable String tenantId,
                                                         @PathVariable String userId,
                                                         @RequestParam int status) {
        return BaseResponse.success(tenantService.setMemberStatus(tenantId, userId, status));
    }

    @PutMapping("/{tenantId}/members/{userId}/density")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<TenantMemberRes> setMemberDensity(@PathVariable String tenantId,
                                                          @PathVariable String userId,
                                                          @RequestParam String density) {
        return BaseResponse.success(tenantService.setMemberDensity(tenantId, userId, density));
    }
}