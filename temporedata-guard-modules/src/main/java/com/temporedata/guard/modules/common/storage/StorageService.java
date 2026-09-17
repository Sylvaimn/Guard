package com.temporedata.guard.modules.common.storage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * 大数据存储抽象层: 本地磁盘 / 对象存储(S3/MinIO 兼容) / HDFS 统一接口。
 * 业务代码只依赖本接口, 通过 storage.type 切换实现(默认 local)。
 */
public interface StorageService {

    /** 存储对象, 返回可访问的 key(相对路径) */
    String store(String key, InputStream in, long size);

    /** 读取对象到输出流, 返回是否成功 */
    boolean read(String key, OutputStream out);

    /** 删除对象 */
    void delete(String key);

    /** 判断对象是否存在 */
    boolean exists(String key);
}
