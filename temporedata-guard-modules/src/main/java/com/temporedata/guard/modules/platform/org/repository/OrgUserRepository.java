package com.temporedata.guard.modules.platform.org.repository;

import com.temporedata.guard.modules.platform.org.entity.OrgUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface OrgUserRepository extends JpaRepository<OrgUserEntity, String> {

    List<OrgUserEntity> findByOrgId(String orgId);

    long countByOrgId(String orgId);

    List<OrgUserEntity> findByOrgIdIn(Collection<String> orgIds);

    List<OrgUserEntity> findByUserId(String userId);

    void deleteByUserId(String userId);

    void deleteByOrgId(String orgId);
}
