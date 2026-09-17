package com.temporedata.guard.modules.platform.tenant.service;
import com.temporedata.guard.modules.platform.org.entity.OrgEntity;
import com.temporedata.guard.modules.platform.org.repository.OrgRepository;
import com.temporedata.guard.modules.platform.org.entity.OrgUserEntity;
import com.temporedata.guard.modules.platform.org.repository.OrgUserRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.tenant.TenantMemberReq;
import com.temporedata.guard.api.tenant.TenantMemberRes;
import com.temporedata.guard.api.tenant.TenantReq;
import com.temporedata.guard.api.tenant.TenantRes;
import com.temporedata.guard.modules.platform.role.entity.RoleEntity;
import com.temporedata.guard.modules.platform.role.repository.RoleRepository;
import com.temporedata.guard.modules.platform.tenant.entity.TenantEntity;
import com.temporedata.guard.modules.platform.tenant.repository.TenantRepository;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tenant management service: CRUD tenants and manage tenant members.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrgUserRepository orgUserRepository;
    private final OrgRepository orgRepository;
    private final PasswordEncoder passwordEncoder;

    public List<TenantRes> listAll() {
        return tenantRepository.findAll().stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    /** 获取租户名称（供各模块展示用） */
    public String getTenantName(String tenantId) {
        return tenantRepository.findById(tenantId).map(t -> t.getName()).orElse("");
    }

    @Transactional
    public TenantRes createTenant(String name, String code, String description,
                                  String contactName, String contactEmail) {
        if (code != null && !code.trim().isEmpty()) {
            boolean exists = tenantRepository.findAll().stream()
                    .anyMatch(t -> code.trim().equals(t.getCode()));
            if (exists) {
                throw new BusinessException("租户编码已存在");
            }
        }
        TenantEntity entity = new TenantEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(name);
        entity.setCode(code);
        entity.setContactName(contactName);
        entity.setContactEmail(contactEmail);
        entity.setRemark(description);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(tenantRepository.save(entity));
    }

    @Transactional
    public TenantRes updateTenant(String id, String name, String description,
                                  String contactName, String contactEmail) {
        TenantEntity entity = tenantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("租户不存在"));
        entity.setName(name);
        entity.setContactName(contactName);
        entity.setContactEmail(contactEmail);
        entity.setRemark(description);
        return toRes(tenantRepository.save(entity));
    }

    @Transactional
    public void deleteTenant(String id) {
        List<UserEntity> members = userRepository.findByTenantId(id);
        members.forEach(m -> orgUserRepository.deleteByUserId(m.getId()));
        userRepository.deleteAll(members);
        // 清理该租户的部门树
        List<OrgEntity> orgs = orgRepository.findByTenantId(id);
        List<String> orgIds = orgs.stream().map(OrgEntity::getId).collect(Collectors.toList());
        if (!orgIds.isEmpty()) {
            orgUserRepository.deleteAll(orgUserRepository.findByOrgIdIn(orgIds));
            orgRepository.deleteAll(orgs);
        }
        tenantRepository.deleteById(id);
    }

    public List<TenantMemberRes> listMembers(String tenantId) {
        return userRepository.findByTenantId(tenantId).stream()
                .map(this::toMemberRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public TenantMemberRes addMember(String tenantId, String username, String password, String roleCode, String orgId) {
        tenantRepository.findById(tenantId)
                .orElseThrow(() -> new BusinessException("租户不存在"));
        if (userRepository.findByUsername(username).isPresent()) {
            throw new BusinessException("用户名已存在");
        }

        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID().toString().replace("-", ""));
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setTenantId(tenantId);
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.now());

        RoleEntity role = roleCode != null && !roleCode.trim().isEmpty()
                ? roleRepository.findByCode(roleCode).orElse(null)
                : roleRepository.findByCode("DEVELOPER").orElse(null);
        if (role != null) {
            user.setRoles(Collections.singleton(role));
        }

        userRepository.save(user);

        if (orgId != null && !orgId.trim().isEmpty()) {
            orgUserRepository.findByUserId(user.getId()).stream()
                    .filter(ou -> ou.getOrgId().equals(orgId))
                    .findFirst()
                    .orElseGet(() -> {
                        OrgUserEntity ou = new OrgUserEntity();
                        ou.setId(UUID.randomUUID().toString().replace("-", ""));
                        ou.setOrgId(orgId);
                        ou.setUserId(user.getId());
                        ou.setCreateTime(LocalDateTime.now());
                        return orgUserRepository.save(ou);
                    });
            // 继承部门默认角色
            orgRepository.findById(orgId)
                    .map(org -> org.getDefaultRoleId() != null
                            ? roleRepository.findById(org.getDefaultRoleId()).orElse(null)
                            : null)
                    .filter(r -> !user.getRoleCodes().contains(r.getCode()))
                    .ifPresent(r -> {
                        Set<RoleEntity> roles = user.getRoles() == null ? new HashSet<>() : new HashSet<>(user.getRoles());
                        roles.add(r);
                        user.setRoles(roles);
                        userRepository.save(user);
                    });
        }
        return toMemberRes(user);
    }

    @Transactional
    public void removeMember(String tenantId, String userId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException("用户不属于该租户");
        }
        orgUserRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    @Transactional
    public TenantMemberRes setMemberStatus(String tenantId, String userId, Integer status) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException("用户不属于该租户");
        }
        user.setStatus(status != null && status == 1 ? 1 : 0);
        return toMemberRes(userRepository.save(user));
    }

    @Transactional
    public TenantMemberRes setMemberDensity(String tenantId, String userId, String density) {
        if (density == null || density.isBlank()) {
            throw new BusinessException("密级不能为空");
        }
        String upper = density.toUpperCase();
        if (!java.util.Set.of("PUBLIC", "INTERNAL", "CONFIDENTIAL", "SECRET").contains(upper)) {
            throw new BusinessException("非法的密级取值：" + density);
        }
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!tenantId.equals(user.getTenantId())) {
            throw new BusinessException("用户不属于该租户");
        }
        user.setDensity(upper);
        return toMemberRes(userRepository.save(user));
    }

    private TenantRes toRes(TenantEntity e) {
        TenantRes res = new TenantRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setCode(e.getCode());
        res.setDescription(e.getRemark());
        res.setContactName(e.getContactName());
        res.setContactEmail(e.getContactEmail());
        res.setStatus("ACTIVE");
        List<UserEntity> members = userRepository.findByTenantId(e.getId());
        res.setMemberCount(members.size());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private TenantMemberRes toMemberRes(UserEntity e) {
        TenantMemberRes res = new TenantMemberRes();
        res.setId(e.getId());
        res.setUsername(e.getUsername());
        res.setRoles(e.getRoleCodes());
        res.setStatus(e.getStatus());
        res.setDensity(e.getDensity());
        List<OrgUserEntity> bindings = orgUserRepository.findByUserId(e.getId());
        if (!bindings.isEmpty()) {
            OrgUserEntity b = bindings.get(0);
            res.setOrgId(b.getOrgId());
            orgRepository.findById(b.getOrgId()).ifPresent(o -> res.setOrgName(o.getName()));
        }
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }
}