package com.temporedata.guard.modules.data.datasource.plugin;

import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.TopicListing;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Kafka 流数据源插件: topic 探测/元数据采集。
 * 懒加载: 运行时存在 kafka-clients 才注册。
 */
@Slf4j
@Component
@ConditionalOnClass(name = "org.apache.kafka.clients.admin.AdminClient")
public class KafkaDataSourcePlugin implements DataSourcePlugin {

    @Override
    public String type() {
        return "KAFKA";
    }

    @Override
    public String[] capabilities() {
        return new String[]{"test", "meta", "topics"};
    }

    private AdminClient admin(DatasourceEntity ds) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, ds.getHost() + ":" + ds.getPort());
        return AdminClient.create(props);
    }

    @Override
    public boolean test(DatasourceEntity ds) {
        try (AdminClient a = admin(ds)) {
            a.describeCluster().nodes().get(5, TimeUnit.SECONDS);
            return true;
        } catch (Exception e) {
            log.warn("Kafka test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> query(DatasourceEntity ds, String sql) {
        return meta(ds);
    }

    @Override
    public List<Map<String, Object>> meta(DatasourceEntity ds) {
        List<Map<String, Object>> topics = new ArrayList<>();
        try (AdminClient a = admin(ds)) {
            a.listTopics().listings().get(5, TimeUnit.SECONDS)
                    .forEach(t -> topics.add(Map.of("name", t.name(), "internal", t.isInternal())));
        } catch (Exception e) {
            log.warn("Kafka meta failed: {}", e.getMessage());
        }
        return topics;
    }
}
