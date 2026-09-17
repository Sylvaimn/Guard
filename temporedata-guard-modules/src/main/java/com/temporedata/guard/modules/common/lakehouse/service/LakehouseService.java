package com.temporedata.guard.modules.common.lakehouse.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.*;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.encryption.EncryptedOutputFile;
import org.apache.iceberg.data.GenericAppenderFactory;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetWriter;
import org.apache.iceberg.hadoop.HadoopCatalog;
import org.apache.iceberg.io.*;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.types.Types;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据湖仓服务(Iceberg): 本地 HadoopCatalog + Parquet 文件格式。
 * warehouse 默认 ./data/lake-iceberg, 支持时间旅行/ACID/快照(Spark 提交器可直接读取)。
 */
@Slf4j
@Service
public class LakehouseService {
    public static final String TABLE = "temporedata_guard_track_flow_daily";
    public static final String NAMESPACE = "temporedata_guard";
    /** 湖仓命名空间与默认表(统一命名) */


    private final String warehouse;
    private final HadoopCatalog catalog;

    public LakehouseService(@Value("${lakehouse.warehouse:./data/lake-iceberg}") String warehouse) {
        this.warehouse = warehouse;
        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", "file:///");
        this.catalog = new HadoopCatalog(conf, Paths.get(warehouse).toAbsolutePath().toString());
        this.catalog.initialize("local", Map.of("warehouse", Paths.get(warehouse).toAbsolutePath().toString()));
    }

    private static final Schema TRACK_SCHEMA = new Schema(
            Types.NestedField.required(1, "region", Types.StringType.get()),
            Types.NestedField.required(2, "stat_date", Types.DateType.get()),
            Types.NestedField.required(3, "renci", Types.IntegerType.get()),
            Types.NestedField.required(4, "guiji", Types.LongType.get())
    );

    /** 建表(若不存在): 轨迹聚合分析表 */
    public String createTable(String tableName) {
        TableIdentifier id = TableIdentifier.of(NAMESPACE, tableName);
        if (!catalog.tableExists(id)) {
            catalog.createTable(id, TRACK_SCHEMA, PartitionSpec.unpartitioned());
        }
        return "ok";
    }

    /** 写入数据(Parquet) */
    public int append(String tableName, List<Map<String, Object>> rows) throws IOException {
        Table table = catalog.loadTable(TableIdentifier.of(NAMESPACE, tableName));
        GenericAppenderFactory appenderFactory = new GenericAppenderFactory(table.schema());
        OutputFile out = table.io().newOutputFile(table.location() + "/data/" + System.currentTimeMillis() + ".parquet");
        EncryptedOutputFile encrypted = table.encryption().encrypt(out);
        DataWriter<Record> writer = appenderFactory.newDataWriter(encrypted, FileFormat.PARQUET, null);
        for (Map<String, Object> r : rows) {
            GenericRecord rec = GenericRecord.create(table.schema());
            rec.setField("region", r.get("region"));
            rec.setField("stat_date", LocalDate.parse(String.valueOf(r.get("stat_date"))));
            rec.setField("renci", ((Number) r.get("renci")).intValue());
            rec.setField("guiji", ((Number) r.get("guiji")).longValue());
            writer.write(rec);
        }
        writer.close();
        table.newAppend().appendFile(writer.toDataFile()).commit();
        return rows.size();
    }

    /** 读取全部行 */
    public List<Map<String, Object>> read(String tableName) throws IOException {
        return read(tableName, null);
    }

    /** 读取行(支持 snapshotId 时间旅行回溯历史快照) */
    public List<Map<String, Object>> read(String tableName, Long snapshotId) throws IOException {
        Table table = catalog.loadTable(TableIdentifier.of(NAMESPACE, tableName));
        List<Map<String, Object>> result = new ArrayList<>();
        IcebergGenerics.ScanBuilder scan = IcebergGenerics.read(table).select("region", "stat_date", "renci", "guiji");
        if (snapshotId != null) {
            Snapshot snap = null;
            for (Snapshot s : table.snapshots()) {
                if (s.snapshotId() == snapshotId.longValue()) {
                    snap = s;
                    break;
                }
            }
            if (snap == null) {
                throw new java.io.IOException("快照不存在: " + snapshotId);
            }
            scan.useSnapshot(snap.snapshotId());
        }
        try (CloseableIterable<Record> it = scan.build()) {
            for (Record rec : it) {
                Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("region", rec.getField("region"));
                m.put("stat_date", String.valueOf(rec.getField("stat_date")));
                m.put("renci", rec.getField("renci"));
                m.put("guiji", rec.getField("guiji"));
                result.add(m);
            }
        }
        return result;
    }

    /** 快照/时间旅行信息 */
    public List<Map<String, Object>> snapshots(String tableName) {
        Table table = catalog.loadTable(TableIdentifier.of(NAMESPACE, tableName));
        List<Map<String, Object>> snaps = new ArrayList<>();
        for (Snapshot s : table.snapshots()) {
            snaps.add(Map.of("snapshotId", s.snapshotId(), "timestamp", s.timestampMillis(), "op", s.operation()));
        }
        return snaps;
    }

    public String getWarehouse() {
        return warehouse;
    }
}
