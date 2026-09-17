package com.temporedata.guard.modules.data.datasource.plugin;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ClickHouse 插件: 列式分析库, 经 HTTP 8123 接口读写(零 JDBC 依赖)。
 */
@Slf4j
@Component
public class ClickHouseDataSourcePlugin implements DataSourcePlugin {

    private final RestTemplate restTemplate = new RestTemplate();

    @org.springframework.beans.factory.annotation.Value("${zy.crypto.key:}")
    private String cryptoKey;

    @Override
    public String type() {
        return "CLICKHOUSE";
    }

    @Override
    public String[] capabilities() {
        return new String[]{"query", "execute", "test", "meta"};
    }

    private String baseUrl(DatasourceEntity ds) {
        int port = ds.getPort() != null && ds.getPort() > 0 ? ds.getPort() : 8123;
        return "http://" + ds.getHost() + ":" + port;
    }
    private HttpEntity<String> entity(DatasourceEntity ds, String body) {
        String plain = com.temporedata.guard.common.util.CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.TEXT_PLAIN);
        String auth = "default:" + plain;
        h.set("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(auth.getBytes()));
        return new HttpEntity<>(body, h);
    }

    @Override
    public boolean test(DatasourceEntity ds) {
        try {
            ResponseEntity<String> r = restTemplate.exchange(baseUrl(ds) + "/ping", HttpMethod.GET, entity(ds, ""), String.class);
            return r.getStatusCode().is2xxSuccessful() && r.getBody() != null && r.getBody().contains("Ok");
        } catch (Exception e) {
            log.warn("ClickHouse test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> query(DatasourceEntity ds, String sql) {
        try {
            String url = baseUrl(ds) + "/?database=" + (ds.getDatabase() == null ? "" : ds.getDatabase());
            ResponseEntity<String> r = restTemplate.exchange(url, HttpMethod.POST, entity(ds, sql + " FORMAT JSONEachRow"), String.class);
            List<Map<String, Object>> rows = new ArrayList<>();
            for (String line : r.getBody().split("\n")) {
                if (line.trim().isEmpty()) continue;
                rows.add(new com.fasterxml.jackson.databind.ObjectMapper().readValue(line, LinkedHashMap.class));
            }
            return rows;
        } catch (Exception e) {
            throw new BusinessException("ClickHouse 查询失败: " + e.getMessage());
        }
    }

    @Override
    public int execute(DatasourceEntity ds, String sql) {
        try {
            String url = baseUrl(ds) + "/";
            ResponseEntity<String> r = restTemplate.exchange(url, HttpMethod.POST, entity(ds, sql), String.class);
            return r.getStatusCode().is2xxSuccessful() ? 1 : 0;
        } catch (Exception e) {
            throw new BusinessException("ClickHouse 执行失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> meta(DatasourceEntity ds) {
        return query(ds, "SELECT name FROM system.tables WHERE database='" + ds.getDatabase() + "'");
    }
}
