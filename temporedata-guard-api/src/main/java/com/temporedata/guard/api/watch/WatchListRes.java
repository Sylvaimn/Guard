package com.temporedata.guard.api.watch;

import lombok.Data;

@Data
public class WatchListRes {

    private String id;

    private String entityId;

    private String entityType;

    private String targetName;

    private String listGroup;

    private Integer level;

    private String reason;

    private String density;

    private String status;

    private String effectiveFrom;

    private String effectiveTo;

    private String createTime;
}
