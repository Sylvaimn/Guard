package com.temporedata.guard.api.behaviorlog;

import lombok.Data;

/**
 * Behavior log request DTO.
 */
@Data
public class BehaviorLogReq {

    private String userId;

    private String username;

    private String action;

    private String targetType;

    private String targetId;

    private String targetName;

    private String detail;

    private String ip;
}