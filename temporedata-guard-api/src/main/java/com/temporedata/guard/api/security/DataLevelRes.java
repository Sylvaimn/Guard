package com.temporedata.guard.api.security;

import lombok.Data;

/**
 * Data sensitivity level response.
 */
@Data
public class DataLevelRes {

    private String id;

    private String name;

    private String code;

    private String description;

    private Integer sortOrder;

    private String createTime;
}