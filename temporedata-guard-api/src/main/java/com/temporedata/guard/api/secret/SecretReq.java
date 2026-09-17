package com.temporedata.guard.api.secret;

import lombok.Data;

/**
 * Create / update secret request.
 */
@Data
public class SecretReq {

    private String key;

    private String value;

    private String description;

    private String scope; // GLOBAL, WORKFLOW
}