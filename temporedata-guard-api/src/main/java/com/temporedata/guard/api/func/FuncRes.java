package com.temporedata.guard.api.func;

import lombok.Data;

@Data
public class FuncRes {

    private String id;

    private String name;

    private String type;

    private String language;

    private String description;

    private String status;

    private String createTime;
}