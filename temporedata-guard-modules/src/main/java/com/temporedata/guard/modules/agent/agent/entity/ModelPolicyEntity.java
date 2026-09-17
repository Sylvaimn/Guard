package com.temporedata.guard.modules.agent.agent.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temporedata_guard_model_policy")
@EntityListeners(AuditingEntityListener.class)
public class ModelPolicyEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 系统级模型策略 JSON(默认模型/参数约束/锁定开关/路由/配额/模型清单) */
    @Lob
    @Column(nullable = false)
    private String policyJson;

    @CreatedDate
    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;
}
