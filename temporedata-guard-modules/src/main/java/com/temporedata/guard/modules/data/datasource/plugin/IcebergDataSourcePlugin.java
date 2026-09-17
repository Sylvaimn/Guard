package com.temporedata.guard.modules.data.datasource.plugin;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Iceberg 湖仓插件: 湖仓表读写/快照/元数据。
 * 懒加载: 仅当运行时存在 Hadoop/Iceberg 时注册(拆分服务无湖仓依赖不注册)。
 */
@Slf4j
@Component
@ConditionalOnClass(name = "org.apache.iceberg.hadoop.HadoopCatalog")
public class IcebergDataSourcePlugin implements DataSourcePlugin {

    @Override
    public String type() {
        return "ICEBERG";
    }

    @Override
    public String[] capabilities() {
        return new String[]{"query", "meta", "snapshots"};
    }

    private HadoopCatalog catalog(DatasourceEntity ds) {
        String warehouse = ds.getHost() != null && !ds.getHost().isBlank()
                ? ds.getHost() : Paths.get("./data/lake-iceberg").toAbsolutePath().toString();
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file:///");
        HadoopCatalog c = new HadoopCatalog(conf, warehouse);
        c.initialize("local", Map.of("warehouse", warehouse));
        return c;
    }

    private String tableName(DatasourceEntity ds) {
        return ds.getDatabase() != null && !ds.getDatabase().isBlank()
                ? ds.getDatabase() + "." + ds.getName() : "police." + ds.getName();
    }

    @Override
    public boolean test(DatasourceEntity ds) {
        try {
            HadoopCatalog c = catalog(ds);
            boolean ok = c.tableExists(TableIdentifier.parse(tableName(ds)));
            log.info("Iceberg test: warehouse={} table={} exists={}", ds.getHost(), tableName(ds), ok);
            return true; // 目录可访问即连通(表可不存在)
        } catch (Exception e) {
            log.warn("Iceberg test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> query(DatasourceEntity ds, String sql) {
        throw new BusinessException("Iceberg 数据源请通过湖仓 API(/api/lakehouse)或 Spark 作业读取");
    }

    @Override
    public List<Map<String, Object>> meta(DatasourceEntity ds) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            HadoopCatalog c = catalog(ds);
            TableIdentifier id = TableIdentifier.parse(tableName(ds));
            if (c.tableExists(id)) {
                Table t = c.loadTable(id);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("name", id.toString());
                m.put("format", "ICEBERG");
                m.put("snapshotCount", t.currentSnapshot() != null ? 1 : 0);
                m.put("snapshotId", t.currentSnapshot() != null ? t.currentSnapshot().snapshotId() : null);
                result.add(m);
            }
        } catch (Exception e) {
            log.warn("Iceberg meta failed: {}", e.getMessage());
        }
        return result;
    }
}
