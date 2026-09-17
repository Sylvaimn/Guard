package com.temporedata.guard.modules.dev.tag.repository;

import com.temporedata.guard.modules.dev.tag.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<TagEntity, String> {

    List<TagEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    List<TagEntity> findByTenantId(String tenantId);

    List<TagEntity> findByClassificationId(String classificationId);
}