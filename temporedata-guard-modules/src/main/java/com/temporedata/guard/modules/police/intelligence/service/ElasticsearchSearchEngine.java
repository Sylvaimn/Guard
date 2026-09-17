package com.temporedata.guard.modules.police.intelligence.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch 检索引擎(可选): zy.search.engine=elasticsearch 时启用。
 * 检索失败(ES 未就绪)时自动降级到数据库引擎，保证服务可用。
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "zy.search", name = "engine", havingValue = "elasticsearch")
public class ElasticsearchSearchEngine implements SearchEngine {

    private static final String INDEX = "temporedata_guard_entity";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityMasterRepository masterRepository;
    private final ObjectProvider<DatabaseSearchEngine> databaseFallback;

    private final String esHost;

    public ElasticsearchSearchEngine(EntityMasterRepository masterRepository,
                                     ObjectProvider<DatabaseSearchEngine> databaseFallback,
                                     @Value("${zy.search.es-host:http://localhost:9200}") String esHost) {
        this.masterRepository = masterRepository;
        this.databaseFallback = databaseFallback;
        this.esHost = esHost;
    }

    @Override
    public String name() {
        return "elasticsearch";
    }

    @Override
    public List<EntityMasterEntity> searchByKeyword(String tenantId, List<String> densities, String keyword, int limit) {
        try {
            Map<String, Object> query = Map.of(
                    "bool", Map.of(
                            "must", List.of(
                                    Map.of("match", Map.of("_all", keyword)),
                                    Map.of("terms", Map.of("tenantId", List.of(tenantId))),
                                    Map.of("terms", Map.of("density", densities)))));
            return search(query, limit);
        } catch (Exception e) {
            log.warn("ES search failed, fallback to database engine: {}", e.getMessage());
            return databaseFallback.getObject().searchByKeyword(tenantId, densities, keyword, limit);
        }
    }

    @Override
    public List<EntityMasterEntity> searchByLabelCode(String tenantId, List<String> densities, String labelCode, int limit) {
        try {
            Map<String, Object> query = Map.of(
                    "bool", Map.of(
                            "must", List.of(
                                    Map.of("term", Map.of("labelCodes", labelCode)),
                                    Map.of("terms", Map.of("tenantId", List.of(tenantId))),
                                    Map.of("terms", Map.of("density", densities)))));
            return search(query, limit);
        } catch (Exception e) {
            log.warn("ES label search failed, fallback to database engine: {}", e.getMessage());
            return databaseFallback.getObject().searchByLabelCode(tenantId, densities, labelCode, limit);
        }
    }

    private List<EntityMasterEntity> search(Map<String, Object> query, int limit) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("query", query);
        body.put("size", limit);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> resp = restTemplate.postForEntity(
                esHost + "/" + INDEX + "/_search",
                new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("ES status " + resp.getStatusCode());
        }
        JsonNode hits = objectMapper.readTree(resp.getBody()).path("hits").path("hits");
        List<String> ids = new ArrayList<>();
        hits.forEach(h -> ids.add(h.path("_id").asText()));
        if (ids.isEmpty()) return List.of();
        // 从主库批量加载实体(保证数据一致性, ES 只做检索加速)
        return masterRepository.findByIdIn(ids);
    }
}
