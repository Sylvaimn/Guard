package com.temporedata.guard.api.func;

import lombok.Data;

@Data
public class FuncReq {

    private String name;

    private String type;

    private String language;

    private String script;

    private String description;
}