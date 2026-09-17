package com.temporedata.guard.api.tenant;

import lombok.Data;

/**
 * Create / update tenant request.
 */
@Data
public class TenantReq {

    private String name;

    private String code;

    private String description;

    private String contactName;

    private String contactEmail;
}