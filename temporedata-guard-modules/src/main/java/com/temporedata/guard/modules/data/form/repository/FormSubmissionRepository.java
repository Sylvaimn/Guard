package com.temporedata.guard.modules.data.form.repository;

import com.temporedata.guard.modules.data.form.entity.FormSubmissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FormSubmissionRepository extends JpaRepository<FormSubmissionEntity, String> {

    List<FormSubmissionEntity> findByFormId(String formId);
}