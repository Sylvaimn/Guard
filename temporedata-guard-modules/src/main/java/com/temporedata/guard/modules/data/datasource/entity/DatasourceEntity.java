package com.temporedata.guard.modules.data.datasource.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * External datasource connection definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_datasource")
public class DatasourceEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, length = 16)
    private String type; // H2, MYSQL, POSTGRESQL

    @Column(length = 128)
    private String host;

    private Integer port;

    @Column(name = "`database`", length = 512)
    private String database;

    @Column(length = 64)
    private String username;

    @Column(length = 512)
    private String password; // AES encrypted

    @Column(length = 256)
    private String params;

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;
}