package com.temporedata.guard.modules.police.photo.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 比对候选（P1）：相似人员候选，必须人工确认/排除。
 */
@Data
@Entity
@Table(name = "temporedata_guard_compare_candidate", indexes = {
        @javax.persistence.Index(name = "idx_cc_task", columnList = "taskId"),
        @javax.persistence.Index(name = "idx_cc_task_status", columnList = "taskId, status")
})
public class CompareCandidateEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String taskId;

    /** 候选人员实体 */
    @Column(nullable = false, length = 32)
    private String entityId;

    /** 相似度 0-1 */
    private Double similarity;

    /** 是否命中布控名单(以图找人→布控联动标注) */
    private Boolean watchHit = false;

    /** PENDING / CONFIRMED / EXCLUDED */
    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    /** 排除原因(排除必填) */
    @Column(length = 256)
    private String reason;

    /** 复核民警 */
    @Column(length = 32)
    private String reviewerId;

    private LocalDateTime reviewTime;
}
