package com.temporedata.guard.api.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Current logged-in user info returned to the frontend.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserInfoRes {

    private String userId;

    private String username;

    private String tenantId;

    private String tenantName;

    private List<String> roles;

    private String density;

    private Boolean mustChangePassword;

    public UserInfoRes(String userId, String username, String tenantId, String tenantName,
                       List<String> roles, String density) {
        this(userId, username, tenantId, tenantName, roles, density, false);
    }
}