package com.temporedata.guard.modules.data.form.repository;

import com.temporedata.guard.modules.data.form.entity.FormEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormRepository extends JpaRepository<FormEntity, String> {

    List<FormEntity> findByTenantIdOrderByCreateTimeDesc(String tenantId);

    FormEntity findByShareToken(String shareToken);
}