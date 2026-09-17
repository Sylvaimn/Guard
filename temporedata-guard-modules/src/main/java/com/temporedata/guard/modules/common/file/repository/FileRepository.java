package com.temporedata.guard.modules.common.file.repository;

import com.temporedata.guard.modules.common.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<FileEntity, String> {

    List<FileEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);
}