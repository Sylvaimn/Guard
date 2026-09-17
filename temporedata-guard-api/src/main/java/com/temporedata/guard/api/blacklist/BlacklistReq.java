package com.temporedata.guard.api.blacklist;

import lombok.Data;

/**
 * Blacklist / whitelist request DTO.
 */
@Data
public class BlacklistReq {

    private String ipAddress;

    private String listType;

    private String reason;

    /** 控制对象类型(IP/CLIENT设备/DEVICE警务通/USER警号) */
    private String targetType;

    /** 来源边界(INTRANET公安内网/GOV政务外网/VPN警务通专网/EXTERNAL交换专线) */
    private String boundary;
}