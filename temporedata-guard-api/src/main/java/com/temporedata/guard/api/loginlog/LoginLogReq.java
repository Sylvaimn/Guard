package com.temporedata.guard.api.loginlog;

import lombok.Data;

/**
 * Login log request DTO.
 */
@Data
public class LoginLogReq {

    private String userId;

    private String username;

    private String loginType;

    private String loginIp;

    private String userAgent;

    private String status;

    private String failReason;
}