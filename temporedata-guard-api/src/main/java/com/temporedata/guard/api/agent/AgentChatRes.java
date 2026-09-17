package com.temporedata.guard.api.agent;

import lombok.Data;

import java.util.List;

/**
 * Agent chat response.
 */
@Data
public class AgentChatRes {

    private String reply;

    private String action; // SUGGEST_SQL, SUGGEST_WORKFLOW, GENERAL

    private List<String> suggestions; // suggested next actions

    private String sessionId; // conversation session id

    private Boolean llm; // whether reply came from LLM

    /** 实体详情卡片(工具链命中实体时下发, 前端结构化渲染) */
    private EntityCardRes entityCard;

    /** 工具链结构化结果(资金/通联/风险等, 前端图表卡片渲染) */
    private Object toolResult;
}