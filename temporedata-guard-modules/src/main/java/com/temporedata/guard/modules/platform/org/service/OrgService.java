package com.temporedata.guard.modules.platform.org.service;
import com.temporedata.guard.modules.platform.org.entity.OrgEntity;
import com.temporedata.guard.modules.platform.org.repository.OrgRepository;
import com.temporedata.guard.modules.platform.org.entity.OrgUserEntity;
import com.temporedata.guard.modules.platform.org.repository.OrgUserRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.org.OrgMemberRes;
import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import com.temporedata.guard.modules.platform.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgRepository orgRepository;
    private final OrgUserRepository orgUserRepository;
    private final UserRepository userRepository;

    public List<Map<String, Object>> tree(String tenantId) {
        List<OrgEntity> all = orgRepository.findByTenantId(tenantId);
        Map<String, List<OrgEntity>> byParent = all.stream()
                .collect(Collectors.groupingBy(o -> isBlank(o.getParentId()) ? "root" : o.getParentId()));
        return buildTree(byParent.getOrDefault("root", Collections.emptyList()), byParent, all);
    }

    private List<Map<String, Object>> buildTree(List<OrgEntity> nodes,
                                                Map<String, List<OrgEntity>> byParent,
                                                List<OrgEntity> all) {
        Map<String, Long> memberCounts = memberCounts(all);
        return nodes.stream().map(n -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", n.getId());
            m.put("name", n.getName());
            m.put("parentId", n.getParentId());
            m.put("remark", n.getRemark());
            m.put("sortOrder", n.getSortOrder());
            m.put("responsibleUserId", n.getResponsibleUserId());
            m.put("defaultRoleId", n.getDefaultRoleId());
            m.put("orgCode", n.getOrgCode());
            m.put("policeType", n.getPoliceType());
            m.put("headcount", n.getHeadcount());
            m.put("orgType", n.getOrgType());
            m.put("expireTime", n.getExpireTime());
            m.put("memberCount", memberCounts.getOrDefault(n.getId(), 0L));
            m.put("createTime", n.getCreateTime());
            m.put("children", buildTree(byParent.getOrDefault(n.getId(), Collections.emptyList()), byParent, all));
            return m;
        }).collect(Collectors.toList());
    }

    private Map<String, Long> memberCounts(List<OrgEntity> orgs) {
        if (orgs.isEmpty()) return Collections.emptyMap();
        Map<String, Long> counts = new HashMap<>();
        for (OrgEntity org : orgs) {
            long c = orgUserRepository.countByOrgId(org.getId());
            if (c > 0) counts.put(org.getId(), c);
        }
        return counts;
    }

    public List<OrgEntity> listByTenant(String tenantId) {
        return orgRepository.findByTenantId(tenantId);
    }

    @Transactional
    public OrgEntity createOrg(String name, String parentId, String tenantId, String remark, String defaultRoleId,
                              String orgCode, String policeType, Integer headcount, String orgType, String expireTime) {
        OrgEntity entity = new OrgEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        entity.setName(name);
        entity.setParentId(isBlank(parentId) ? null : parentId);
        entity.setTenantId(tenantId);
        entity.setRemark(remark);
        entity.setDefaultRoleId(defaultRoleId);
        entity.setOrgCode(orgCode);
        entity.setPoliceType(policeType);
        entity.setHeadcount(headcount);
        entity.setOrgType(orgType);
        entity.setExpireTime(expireTime);
        entity.setSortOrder(0);
        entity.setCreateTime(LocalDateTime.now());
        return orgRepository.save(entity);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    @Transactional
    public OrgEntity updateOrg(String id, String name, String parentId, String remark, String responsibleUserId, String defaultRoleId,
                              String orgCode, String policeType, Integer headcount, String orgType, String expireTime) {
        OrgEntity entity = orgRepository.findById(id)
                .orElseThrow(() -> new BusinessException("组织不存在"));
        if (name != null) entity.setName(name);
        if (parentId != null) entity.setParentId(isBlank(parentId) ? null : parentId);
        if (remark != null) entity.setRemark(remark);
        if (responsibleUserId != null) entity.setResponsibleUserId(responsibleUserId);
        if (defaultRoleId != null) entity.setDefaultRoleId(defaultRoleId);
        if (orgCode != null) entity.setOrgCode(orgCode);
        if (policeType != null) entity.setPoliceType(policeType);
        if (headcount != null) entity.setHeadcount(headcount);
        if (orgType != null) entity.setOrgType(orgType);
        if (expireTime != null) entity.setExpireTime(expireTime);
        return orgRepository.save(entity);
    }

    @Transactional
    public void deleteOrg(String id) {
        List<OrgEntity> children = orgRepository.findByParentId(id);
        if (!children.isEmpty()) {
            throw new BusinessException("该组织下有子部门，请先删除子部门");
        }
        long memberCount = orgUserRepository.findByOrgId(id).size();
        if (memberCount > 0) {
            throw new BusinessException("该部门下仍有 " + memberCount + " 名成员，请先移除成员");
        }
        orgUserRepository.deleteByOrgId(id);
        orgRepository.deleteById(id);
    }

    /**
     * Assign member to department.
     */
    @Transactional
    public boolean assignMember(String orgId, String userId) {
        orgRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException("组织不存在"));
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (orgUserRepository.findByUserId(userId).stream()
                .anyMatch(ou -> ou.getOrgId().equals(orgId))) {
            return false;
        }
        OrgUserEntity ou = new OrgUserEntity();
        ou.setId(UUID.randomUUID().toString().replace("-", ""));
        ou.setOrgId(orgId);
        ou.setUserId(userId);
        ou.setCreateTime(LocalDateTime.now());
        orgUserRepository.save(ou);
        return true;
    }

    /**
     * Remove member from department.
     */
    @Transactional
    public void removeMember(String orgId, String userId) {
        Optional<OrgUserEntity> binding = orgUserRepository.findByUserId(userId).stream()
                .filter(ou -> ou.getOrgId().equals(orgId))
                .findFirst();
        if (binding.isPresent()) {
            orgUserRepository.deleteById(binding.get().getId());
        } else {
            throw new BusinessException("成员不属于该部门");
        }
    }

    /**
     * Transfer member to a new department.
     */
    @Transactional
    public void transferMember(String userId, String newOrgId) {
        orgRepository.findById(newOrgId)
                .orElseThrow(() -> new BusinessException("目标组织不存在"));
        orgUserRepository.deleteByUserId(userId);
        assignMember(newOrgId, userId);
    }

    /**
     * Department member list.
     */
    @Transactional(readOnly = true)
    public List<OrgMemberRes> members(String orgId) {
        orgRepository.findById(orgId)
                .orElseThrow(() -> new BusinessException("组织不存在"));
        List<OrgUserEntity> bindings = orgUserRepository.findByOrgId(orgId);
        if (bindings.isEmpty()) return Collections.emptyList();
        List<String> userIds = bindings.stream().map(OrgUserEntity::getUserId).collect(Collectors.toList());
        Map<String, UserEntity> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserEntity::getId, u -> u));
        return bindings.stream()
                .map(b -> usersById.get(b.getUserId()))
                .filter(Objects::nonNull)
                .map(this::toMemberRes)
                .collect(Collectors.toList());
    }

    private OrgMemberRes toMemberRes(UserEntity u) {
        OrgMemberRes res = new OrgMemberRes();
        res.setId(u.getId());
        res.setUsername(u.getUsername());
        res.setRoles(u.getRoleCodes());
        res.setTenantId(u.getTenantId());
        res.setCreateTime(u.getCreateTime() != null ? u.getCreateTime().toString() : null);
        return res;
    }
}
