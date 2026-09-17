package com.temporedata.guard.modules.police.intelligence.service;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMappingRepository;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;
import com.temporedata.guard.modules.agent.aianalysis.vector.VectorStore;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.common.async.TaskExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 实体索引同步(写路径): 实体注册/删除/确认/合并/标签变更后异步同步到检索引擎。
 * 仅当 zy.search.engine=elasticsearch 时启用；ES 不可用时静默降级(日志 warn)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EntityIndexService {

    private static final String INDEX = "temporedata_guard_entity";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final EntityMasterRepository masterRepository;
    private final EntityLabelRepository labelRepository;
    private final EntityMappingRepository mappingRepository;
    private final TaskExecutorService taskExecutorService;
    private final org.springframework.beans.factory.ObjectProvider<com.temporedata.guard.modules.agent.aianalysis.vector.VectorStore> vectorStoreProvider;

    @Value("${zy.search.engine:database}")
    private String searchEngine;

    @Value("${zy.search.es-host:http://localhost:9200}")
    private String esHost;

    /** 同步单个实体到索引(异步, 不阻塞主流程) */
    public void syncAsync(String entityId) {
        if (!"elasticsearch".equals(searchEngine)) return;
        taskExecutorService.submit("es-sync:" + entityId, () -> doSync(entityId));
    }

    /** 从索引删除(异步) */
    public void deleteAsync(String entityId) {
        if (!"elasticsearch".equals(searchEngine)) return;
        taskExecutorService.submit("es-del:" + entityId, () -> doDelete(entityId));
    }

    private void doSync(String entityId) {
        try {
            EntityMasterEntity e = masterRepository.findById(entityId).orElse(null);
            if (e == null) return;
            List<String> labelCodes = labelRepository.findByEntityId(entityId).stream()
                    .map(l -> l.getLabelCode()).collect(Collectors.toList());
            // 向量库同步(智能分析语义检索, 可选)
            try {
                com.temporedata.guard.modules.agent.aianalysis.vector.VectorStore vs = vectorStoreProvider.getIfAvailable();
                if (vs != null && vs.available()) {
                    vs.upsertEntity(e, labelCodes);
                }
            } catch (Exception v) {
                log.debug("Vector upsert skipped: {}", v.getMessage());
            }
            if (!"elasticsearch".equals(searchEngine)) return;

            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("entityType", e.getEntityType());
            doc.put("displayName", e.getDisplayName());
            doc.put("primaryKey", e.getPrimaryKey());
            doc.put("density", e.getDensity());
            doc.put("status", e.getStatus());
            doc.put("tenantId", e.getTenantId());
            doc.put("labelCodes", labelCodes);
            doc.put("sourceSystems", mappingRepository.findByEntityId(entityId).stream()
                    .map(m -> m.getSourceSystem()).distinct().collect(Collectors.toList()));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                    esHost + "/" + INDEX + "/_doc/" + entityId,
                    new HttpEntity<>(objectMapper.writeValueAsString(doc), headers), String.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                log.debug("ES index synced: {}", entityId);
            } else {
                log.warn("ES index sync status: {} for {}", resp.getStatusCode(), entityId);
            }
        } catch (Exception ex) {
            log.warn("ES index sync failed (ignored): {} - {}", entityId, ex.getMessage());
        }
    }

    private void doDelete(String entityId) {
        try {
            restTemplate.delete(esHost + "/" + INDEX + "/_doc/" + entityId);
        } catch (Exception ex) {
            log.warn("ES index delete failed (ignored): {} - {}", entityId, ex.getMessage());
        }
    }
}
