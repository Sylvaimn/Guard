package com.temporedata.guard.api.passwordless;

import lombok.Data;

/**
 * Passwordless config request DTO.
 */
@Data
public class PasswordlessConfigReq {

    private String loginType;

    private String config;
}