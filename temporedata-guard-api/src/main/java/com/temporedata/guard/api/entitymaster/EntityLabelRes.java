package com.temporedata.guard.api.entitymaster;

import lombok.Data;

@Data
public class EntityLabelRes {

    private String id;

    private String entityId;

    private String labelType;

    private String labelCode;

    private String labelName;

    private String source;

    private Double confidence;

    private String expireTime;
}
