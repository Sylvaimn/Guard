package com.temporedata.guard.modules.dev.func.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "temporedata_guard_func")
public class FuncEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 128)
    private String name;

    @Column(nullable = false, length = 16)
    private String type; // UDF, UDAF, UDTF

    @Column(nullable = false, length = 16)
    private String language; // JAVA, PYTHON

    @Lob
    @Column(nullable = false)
    private String script;

    @Column(length = 512)
    private String description;

    @Column(nullable = false, length = 16)
    private String status; // ACTIVE, INACTIVE

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}