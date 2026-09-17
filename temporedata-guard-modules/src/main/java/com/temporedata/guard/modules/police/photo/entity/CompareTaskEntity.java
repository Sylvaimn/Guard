package com.temporedata.guard.modules.police.photo.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 照片比对任务（P1）：以图找人闭环主表。
 * 状态: PENDING/MATCHING/REVIEWING/CONFIRMED/EXCLUDED
 */
@Data
@Entity
@Table(name = "temporedata_guard_compare_task", indexes = {
        @javax.persistence.Index(name = "idx_ct_tenant_status", columnList = "tenantId, status"),
        @javax.persistence.Index(name = "idx_ct_tenant_time", columnList = "tenantId, createTime")
})
public class CompareTaskEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 比对照片(file 模块 fileId) */
    @Column(nullable = false, length = 32)
    private String fileId;

    /** 关联案件(可空) */
    @Column(length = 32)
    private String caseId;

    /** 比对库: ALL(全部)/WANTED(在逃)/RECORD(前科)/ARCHIVE(失踪) */
    @javax.persistence.Column(name = "`database`", length = 32)
    private String database = "ALL";

    /** 照片来源: SCENE(现场)/MONITOR(监控)/IDCARD(证件)/MUG(前科) */
    @Column(nullable = false, length = 16)
    private String source = "SCENE";

    /** 民警线索词(P1 降级特征: 车牌/姓名/特征描述, OCR/CLIP 上线后可为空) */
    @Column(length = 256)
    private String keywords;

    /** 算法标识: OCR_KM(关键词降级)/LOCAL_FEATURE/CLIP */
    @Column(nullable = false, length = 32)
    private String algorithm = "OCR_KM";

    @Column(nullable = false, length = 16)
    private String status = "PENDING";

    /** 发起民警 */
    @Column(nullable = false, length = 32)
    private String operatorId;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime finishTime;
}
