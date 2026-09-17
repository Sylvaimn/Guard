package com.temporedata.guard.modules.common.work.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Execution instance of a work job.
 * Tracks each run's status, logs, and timing.
 */
@Data
@Entity
@Table(name = "temporedata_guard_work_instance")
public class WorkInstanceEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String workId;

    @Column(length = 32)
    private String workflowInstanceId;

    @Column(length = 32)
    private String status; // RUNNING, SUCCESS, FAIL, STOPPED

    @Column(columnDefinition = "TEXT")
    private String submitLog;

    @Column(columnDefinition = "TEXT")
    private String yarnLog;

    @Column(columnDefinition = "TEXT")
    private String resultData;

    private LocalDateTime execStartDateTime;

    private LocalDateTime execEndDateTime;

    private Long duration;

    @Column(length = 32)
    private String tenantId;

    private LocalDateTime createDateTime;
}