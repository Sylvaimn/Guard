package com.temporedata.guard.api.loginlog;

import lombok.Data;

/**
 * Login log response DTO.
 */
@Data
public class LoginLogRes {

    private String id;

    private String userId;

    private String username;

    private String loginType;

    private String loginIp;

    private String userAgent;

    private String status;

    private String failReason;

    private String tenantId;

    private String createTime;
}