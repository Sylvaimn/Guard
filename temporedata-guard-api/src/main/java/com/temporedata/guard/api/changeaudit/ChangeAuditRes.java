package com.temporedata.guard.api.changeaudit;

import lombok.Data;

/**
 * Asset-level change audit record.
 */
@Data
public class ChangeAuditRes {

    private String id;

    private String entityType; // TABLE, INDICATOR, TAG, GLOSSARY, GLOSSARY_TERM, DATASOURCE

    private String entityId;

    private String entityName;

    private String action; // CREATE, UPDATE, DELETE, BIND, UNBIND, EXECUTE, APPROVE, REJECT, SUBMIT

    private String operator;

    private String beforeValue;

    private String afterValue;

    private String createTime;
}