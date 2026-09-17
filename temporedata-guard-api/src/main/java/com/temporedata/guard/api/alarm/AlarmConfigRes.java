package com.temporedata.guard.api.alarm;

import lombok.Data;

/**
 * Alarm config list / detail response.
 */
@Data
public class AlarmConfigRes {

    private String id;

    private String name;

    private String eventType;

    private String channels;

    private Boolean enabled;

    private String createTime;
}