package com.temporedata.guard.api.datasource;

import lombok.Data;

/**
 * Create / update datasource request.
 */
@Data
public class DatasourceReq {

    private String name;

    private String type; // H2, MYSQL, POSTGRESQL

    private String host;

    private Integer port;

    private String database;

    private String username;

    private String password;

    private String params; // extra JDBC params
}