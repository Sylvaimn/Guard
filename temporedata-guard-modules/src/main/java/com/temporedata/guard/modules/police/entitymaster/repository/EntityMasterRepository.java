package com.temporedata.guard.modules.police.entitymaster.repository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EntityMasterRepository extends JpaRepository<EntityMasterEntity, String> {

    List<EntityMasterEntity> findByEntityTypeAndTenantIdOrderByCreateTimeDesc(String entityType, String tenantId);

    List<EntityMasterEntity> findByStatusAndTenantIdOrderByCreateTimeDesc(String status, String tenantId);

    List<EntityMasterEntity> findAllByTenantIdOrderByCreateTimeDesc(String tenantId);

    Optional<EntityMasterEntity> findFirstByPrimaryKeyAndEntityType(String primaryKey, String entityType);

    List<EntityMasterEntity> findByIdIn(Collection<String> ids);

    /** 关键字检索: 姓名/主标识模糊 + 租户 + 密级下推 */
    @Query("select e from EntityMasterEntity e where e.tenantId = :tenantId and e.density in :densities " +
            "and (lower(e.displayName) like %:kw% or lower(e.primaryKey) like %:kw%)")
    List<EntityMasterEntity> searchByKeyword(@Param("tenantId") String tenantId,
                                             @Param("densities") Collection<String> densities,
                                             @Param("kw") String kw, Pageable pageable);

    /** 标签条件检索: 按标签码匹配 + 租户 + 密级下推 */
    @Query("select distinct e from EntityMasterEntity e join EntityLabelEntity l on l.entityId = e.id " +
            "where e.tenantId = :tenantId and e.density in :densities and l.labelCode = :labelCode")
    List<EntityMasterEntity> searchByLabelCode(@Param("tenantId") String tenantId,
                                               @Param("densities") Collection<String> densities,
                                               @Param("labelCode") String labelCode, Pageable pageable);
}
