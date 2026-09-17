package com.temporedata.guard.modules.dev.engine.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.dev.engine.entity.SparkJobEntity;
import com.temporedata.guard.modules.dev.engine.repository.SparkJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 湖仓作业模板服务: 5 类预置模板(Spark 入湖/湖仓加工/分析加载/Flink 流式入湖/湖仓导出)。
 * 模板生成可执行的 SQL 骨架(占位符替换), 落库为引擎作业, 由提交器实连执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EngineTemplateService {

    private final SparkJobRepository sparkJobRepository;

    private static final List<Map<String, Object>> TEMPLATES = List.of(
            Map.of("id", "spark-to-lake", "name", "Spark → Iceberg 入湖",
                    "type", "SPARK_SQL", "engine", "Spark",
                    "desc", "读取源库 SQL → 写入湖仓(temporedata_guard.*, Parquet + 快照)",
                    "sql", "INSERT INTO lake.temporedata_guard.temporedata_guard_track_flow_daily (region, stat_date, renci, guiji)\nSELECT region, stat_date, SUM(renci) renci, SUM(guiji) guiji\nFROM ${sourceTable}\nWHERE stat_date = '${statDate}'\nGROUP BY region, stat_date"),
            Map.of("id", "spark-elt", "name", "Spark 湖仓加工",
                    "type", "SPARK_SQL", "engine", "Spark",
                    "desc", "湖仓表间 ETL(下推引擎执行, 生成新快照)",
                    "sql", "CREATE TABLE lake.police.${targetTable} AS\nSELECT region, stat_date, renci * 1.0 / NULLIF(guiji, 0) AS avg_flow\nFROM lake.temporedata_guard.temporedata_guard_track_flow_daily\nWHERE stat_date = '${statDate}'"),
            Map.of("id", "spark-to-ch", "name", "Spark → ClickHouse 分析加载",
                    "type", "SPARK_SQL", "engine", "Spark",
                    "desc", "湖仓聚合结果 → ClickHouse 分析层(temporedata_guard_analysis)",
                    "sql", "INSERT INTO clickhouse.temporedata_guard_analysis\nSELECT region, stat_date, SUM(renci) renci, SUM(guiji) guiji\nFROM lake.temporedata_guard.temporedata_guard_track_flow_daily\nGROUP BY region, stat_date"),
            Map.of("id", "flink-stream-lake", "name", "Flink 流式入湖",
                    "type", "FLINK_SQL", "engine", "Flink",
                    "desc", "Kafka 事件流(tdg-cdc) → Iceberg 湖仓(实时链路)",
                    "sql", "CREATE TABLE kafka_cdc (\n  region STRING, stat_date STRING, renci INT, guiji BIGINT\n) WITH (\n  'connector' = 'kafka', 'topic' = '${topic}', 'properties.bootstrap.servers' = '${kafkaServers}'\n);\nINSERT INTO lake.temporedata_guard.temporedata_guard_track_flow_daily\nSELECT * FROM kafka_cdc;"),
            Map.of("id", "lake-export", "name", "湖仓导出",
                    "type", "SPARK_SQL", "engine", "Spark",
                    "desc", "湖仓表 → 对象存储/文件(StorageService 复用)",
                    "sql", "INSERT OVERWRITE DIRECTORY '${exportPath}'\nUSING parquet\nSELECT * FROM lake.temporedata_guard.temporedata_guard_track_flow_daily\nWHERE stat_date = '${statDate}'"));

    public List<Map<String, Object>> list() {
        return TEMPLATES;
    }

    /** 从模板创建作业: 参数替换占位符 -> 落库为 Spark 作业 */
    public String createJob(String templateId, Map<String, String> params, String tenantId) {
        Map<String, Object> tpl = TEMPLATES.stream()
                .filter(t -> templateId.equals(t.get("id")))
                .findFirst()
                .orElseThrow(() -> new BusinessException("模板不存在: " + templateId));
        String sql = String.valueOf(tpl.get("sql"));
        if (params != null) {
            for (Map.Entry<String, String> e : params.entrySet()) {
                sql = sql.replace("${" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
            }
        }
        SparkJobEntity job = new SparkJobEntity();
        job.setId(UUID.randomUUID().toString().replace("-", ""));
        job.setName(params != null ? params.getOrDefault("jobName", String.valueOf(tpl.get("name"))) : String.valueOf(tpl.get("name")));
        job.setType("SPARK_SQL");
        job.setTenantId(tenantId);
        job.setSqlContent(sql);
        job.setStatus("DRAFT");
        job.setCreateTime(java.time.LocalDateTime.now());
        sparkJobRepository.save(job);
        log.info("[ENGINE-TEMPLATE] 作业已从模板创建: {} -> {}", templateId, job.getName());
        return job.getId();
    }
}
