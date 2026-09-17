package com.temporedata.guard.modules.platform.role.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.role.RoleMemberRes;
import com.temporedata.guard.modules.platform.role.entity.RoleEntity;
import com.temporedata.guard.modules.platform.role.repository.RoleRepository;
import com.temporedata.guard.modules.platform.tenant.entity.TenantEntity;
import com.temporedata.guard.modules.platform.tenant.repository.TenantRepository;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;

    /** 内置角色：禁止删除、禁止修改编码 */
    private static final List<String> BUILTIN_CODES = List.of("ADMIN", "DEVELOPER");

    public List<RoleEntity> listAll() {
        return roleRepository.findAll();
    }

    public RoleEntity getByCode(String code) {
        return roleRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException("角色不存在: " + code));
    }

    @Transactional
    public RoleEntity createRole(String code, String name, String remark, String dataScope) {
        if (roleRepository.findByCode(code).isPresent()) {
            throw new BusinessException("角色编码已存在");
        }
        RoleEntity entity = new RoleEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setCode(code);
        entity.setName(name);
        entity.setRemark(remark);
        entity.setProtectedRole(BUILTIN_CODES.contains(code));
        entity.setDataScope(dataScope != null ? dataScope : "UNIT");
        entity.setCreateTime(LocalDateTime.now());
        return roleRepository.save(entity);
    }

    @Transactional
    public RoleEntity updateRole(String id, String code, String name, String remark, String dataScope) {
        RoleEntity entity = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        if (entity.getProtectedRole() != null && entity.getProtectedRole()) {
            if (code != null && !code.equals(entity.getCode())) {
                throw new BusinessException("内置角色不允许修改编码");
            }
        }
        if (code != null) entity.setCode(code);
        if (name != null) entity.setName(name);
        if (remark != null) entity.setRemark(remark);
        if (dataScope != null) entity.setDataScope(dataScope);
        return roleRepository.save(entity);
    }

    @Transactional
    public void deleteRole(String id) {
        RoleEntity entity = roleRepository.findById(id)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        if (entity.getProtectedRole() != null && entity.getProtectedRole()) {
            throw new BusinessException("内置角色不允许删除");
        }
        List<UserEntity> holders = userRepository.findByRoleId(id);
        if (!holders.isEmpty()) {
            throw new BusinessException("该角色下仍有 " + holders.size() + " 个用户，请先移除其角色");
        }
        roleRepository.deleteById(id);
    }

    public List<RoleMemberRes> members(String roleId) {
        roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException("角色不存在"));
        return userRepository.findByRoleId(roleId).stream()
                .map(this::toMemberRes)
                .collect(Collectors.toList());
    }

    private RoleMemberRes toMemberRes(UserEntity u) {
        RoleMemberRes res = new RoleMemberRes();
        res.setId(u.getId());
        res.setUsername(u.getUsername());
        res.setTenantId(u.getTenantId());
        Optional<TenantEntity> tenant = tenantRepository.findById(u.getTenantId());
        res.setTenantName(tenant.map(TenantEntity::getName).orElse(null));
        res.setCreateTime(u.getCreateTime() != null ? u.getCreateTime().toString() : null);
        return res;
    }
}
