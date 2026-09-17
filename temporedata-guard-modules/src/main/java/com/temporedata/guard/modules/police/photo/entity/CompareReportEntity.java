package com.temporedata.guard.modules.police.photo.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 比对报告（P1）：取证留档(操作人/时间/算法/候选与复核结果)。
 */
@Data
@Entity
@Table(name = "temporedata_guard_compare_report", indexes = {
        @javax.persistence.Index(name = "idx_cr_task", columnList = "taskId")
})
public class CompareReportEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String taskId;

    /** 报告内容(JSON: 任务信息/算法/候选及复核结果) */
    @Column(length = 8000)
    private String reportContent;

    /** 导出文件(fileId, 可空) */
    @Column(length = 32)
    private String fileId;

    @Column(nullable = false, length = 32)
    private String creatorId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}
