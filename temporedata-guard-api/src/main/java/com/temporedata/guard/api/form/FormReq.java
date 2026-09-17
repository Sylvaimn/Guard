package com.temporedata.guard.api.form;

import lombok.Data;

/**
 * Create / update form request.
 */
@Data
public class FormReq {

    private String name;

    private String description;

    private String config; // JSON form schema

    /** 分类(CLUE线索登记/PERSON人员采集/PLACE场所检查/DUTY接警登记) */
    private String category;

    /** 是否涉敏感字段(涉敏感默认需审批) */
    private Boolean sensitive;
}