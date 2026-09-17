package com.temporedata.guard.modules.platform.role.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.role.RoleMemberRes;
import com.temporedata.guard.modules.platform.role.entity.RoleEntity;
import com.temporedata.guard.modules.platform.role.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/role")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<RoleEntity> list() {
        return roleService.listAll();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{id}/members")
    public BaseResponse<List<RoleMemberRes>> members(@PathVariable String id) {
        return BaseResponse.success(roleService.members(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public BaseResponse<RoleEntity> create(@RequestBody Map<String, String> body) {
        return BaseResponse.success(roleService.createRole(body.get("code"), body.get("name"), body.get("remark"), body.get("dataScope")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public BaseResponse<RoleEntity> update(@PathVariable String id, @RequestBody Map<String, String> body) {
        return BaseResponse.success(roleService.updateRole(id, body.get("code"), body.get("name"), body.get("remark"), body.get("dataScope")));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        roleService.deleteRole(id);
        return BaseResponse.success();
    }
}
