package com.temporedata.guard.api.file;

import lombok.Data;

@Data
public class FileRes {

    private String id;

    private String name;

    private String originalName;

    private Long fileSize;

    private String fileType;

    private String createTime;
}