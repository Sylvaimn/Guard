package com.temporedata.guard.modules.platform.user.repository;

import com.temporedata.guard.modules.platform.user.entity.UserEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    @EntityGraph(attributePaths = "roles")
    Optional<UserEntity> findByUsername(String username);

    @EntityGraph(attributePaths = "roles")
    List<UserEntity> findByTenantId(String tenantId);

    @Query("select u from UserEntity u join u.roles r where r.id = :roleId")
    List<UserEntity> findByRoleId(@Param("roleId") String roleId);
}