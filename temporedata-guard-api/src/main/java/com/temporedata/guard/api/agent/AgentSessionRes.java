package com.temporedata.guard.api.agent;

import lombok.Data;

/**
 * Agent chat session summary.
 */
@Data
public class AgentSessionRes {

    private String sessionId;

    private String title;

    private Integer messageCount;

    /** 会话场景分组 */
    private String scene;

    /** 未读条数(ASSISTANT 消息) */
    private Integer unreadCount = 0;

    private String lastTime;
}