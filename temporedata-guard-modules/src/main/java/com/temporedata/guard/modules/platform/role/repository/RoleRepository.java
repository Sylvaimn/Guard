package com.temporedata.guard.modules.platform.role.repository;

import com.temporedata.guard.modules.platform.role.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, String> {

    Optional<RoleEntity> findByCode(String code);
}