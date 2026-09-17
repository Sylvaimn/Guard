package com.temporedata.guard.modules.common.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 存储配置: storage.type=local|s3
 */
@Data
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /** local 本地目录 / s3 对象存储(MinIO 兼容) */
    private String type = "local";

    /** 本地存储根目录 */
    private String localDir = "./data/files";

    /** 对象存储端点, 如 http://minio:9000 */
    private String endpoint = "";

    /** 桶名 */
    private String bucket = "tdg";

    /** 访问令牌(MinIO 直连场景) */
    private String token = "";
}
