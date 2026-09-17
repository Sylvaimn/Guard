package com.temporedata.guard.modules.data.form.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Form submission record.
 */
@Data
@Entity
@Table(name = "temporedata_guard_form_submission")
public class FormSubmissionEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String formId;

    @Column(length = 8192)
    private String data; // JSON submission data

    @Column(length = 64)
    private String submitterIp;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime submitTime;
}