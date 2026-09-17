package com.temporedata.guard.modules.data.datasource.plugin;
import com.temporedata.guard.modules.data.datasource.service.DatasourceService;

import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;

import java.util.List;
import java.util.Map;

/**
 * 数据源插件 SPI: 新数据源类型(Doris/StarRocks/ES...)只需实现本接口并注册为 Spring Bean,
 * 主流程(DatasourceService/PluginRegistry)零改动。
 * 能力: 连通测试 / 查询 / 写执行 / 元数据采集 / 能力声明。
 */
public interface DataSourcePlugin {

    /** 类型标识, 对应 DatasourceEntity.type */
    String type();

    /** 别名(如 MYSQL/H2/POSTGRESQL 均指向 JDBC 插件), 默认空 */
    default java.util.List<String> aliases() {
        return java.util.List.of();
    }

    /** 支持的能力标记(前端按能力渲染操作) */
    default String[] capabilities() {
        return new String[]{"query", "test"};
    }

    /** 类型定制连通测试 */
    boolean test(DatasourceEntity ds);

    /** 查询执行(返回行集合) */
    List<Map<String, Object>> query(DatasourceEntity ds, String sql);

    /** 写执行(返回影响行数) */
    default int execute(DatasourceEntity ds, String sql) {
        throw new UnsupportedOperationException("该数据源类型不支持写执行: " + type());
    }

    /** 元数据采集(库/表/分区等, 供元数据模块) */
    default List<Map<String, Object>> meta(DatasourceEntity ds) {
        return List.of();
    }

    /** 资源释放(可选) */
    default void release(DatasourceEntity ds) {
    }
}
