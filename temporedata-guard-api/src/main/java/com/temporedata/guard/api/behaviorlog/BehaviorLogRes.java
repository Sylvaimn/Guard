package com.temporedata.guard.api.behaviorlog;

import lombok.Data;

/**
 * Behavior log response DTO.
 */
@Data
public class BehaviorLogRes {

    private String id;

    private String userId;

    private String username;

    private String action;

    private String targetType;

    private String targetId;

    private String targetName;

    private String detail;

    private String ip;

    private String tenantId;

    private String createTime;
}