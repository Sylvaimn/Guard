package com.temporedata.guard.api.watch;

import lombok.Data;

@Data
public class WatchAlertRes {

    private String id;

    private String watchListId;

    private String entityId;

    private String targetName;

    private String hitScene;

    private String hitSource;

    private Integer level;

    private String density;

    private String status;

    private String assignee;

    private String feedback;

    private String result;

    private String merit;

    private String createTime;

    private String assignTime;

    private String closeTime;
}
