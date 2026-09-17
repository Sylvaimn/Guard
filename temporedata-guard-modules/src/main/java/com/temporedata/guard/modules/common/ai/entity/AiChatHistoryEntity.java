package com.temporedata.guard.modules.common.ai.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * AI chat history entity.
 */
@Data
@Entity
@Table(name = "temporedata_guard_ai_chat_history")
public class AiChatHistoryEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 32)
    private String userId;

    @Column(length = 32)
    private String sessionId;

    @Column(length = 16)
    private String role;

    @Column(nullable = false, length = 8192)
    private String content;

    @Column(length = 64)
    private String modelName;

    private Integer tokenCount;

    @Column(nullable = false, length = 32)
    private String tenantId;

    /** 会话场景分组(案件线索/人员检索/车辆查询/舆情/研判报告) */
    @Column(length = 32)
    private String scene;

    /** 已读标记(ASSISTANT 消息, 会话选中时批量置读) */
    private Boolean readFlag = false;

    @Column(nullable = false)
    private LocalDateTime createTime;
}