package com.temporedata.guard.modules.police.intelligence.service;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityLabelEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMappingEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMappingRepository;
import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;

import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.security.security.DensityGuard;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 情报检索（研判辅助）：实体联合检索（姓名/主标识/标签），行级密级过滤 + 脱敏。
 * V2 优化：SQL 下推(租户/密级/关键字)，标签与来源映射批量加载，消除全表扫描与 N+1。
 * 检索走可插拔 SearchEngine(默认数据库, 可切 Elasticsearch)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IntelligenceService {

    private static final int SEARCH_LIMIT = 50;
    private static final int RAG_LIMIT = 5;

    private final EntityMasterRepository masterRepository;
    private final EntityLabelRepository labelRepository;
    private final EntityMappingRepository mappingRepository;
    private final DatabaseSearchEngine databaseSearchEngine;
    private final org.springframework.beans.factory.ObjectProvider<ElasticsearchSearchEngine> esEngineProvider;

    /** 当前生效的检索引擎(ES 启用则用 ES, 否则数据库) */
    private SearchEngine activeEngine() {
        ElasticsearchSearchEngine es = esEngineProvider.getIfAvailable();
        return es != null ? es : databaseSearchEngine;
    }

    public List<IntelligenceRes> search(String keyword, String labelCode, LoginUser user) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        String kw = keyword.trim().toLowerCase();
        List<String> densities = DensityGuard.allowedDensities(user.getDensity());
        SearchEngine engine = activeEngine();

        List<EntityMasterEntity> entities;
        if (labelCode != null && !labelCode.isBlank()) {
            // 标签检索：关键字不匹配姓名/主标识时, 按标签码精确匹配
            List<EntityMasterEntity> byKeyword = engine.searchByKeyword(user.getTenantId(), densities, kw, SEARCH_LIMIT);
            entities = byKeyword.isEmpty()
                    ? engine.searchByLabelCode(user.getTenantId(), densities, labelCode, SEARCH_LIMIT)
                    : byKeyword;
        } else {
            entities = engine.searchByKeyword(user.getTenantId(), densities, kw, SEARCH_LIMIT);
        }
        // 检索结果为批量展示场景: 主标识一律脱敏(默认最小化), 明细见实体主数据接口
        return toResList(entities);
    }

    /** Agent RAG 上下文：检索实体情报摘要（脱敏后拼接） */
    public String buildRagContext(String keyword, String tenantId, String userDensity) {
        if (keyword == null || keyword.isBlank()) return "";
        String kw = keyword.trim().toLowerCase();
        List<String> densities = DensityGuard.allowedDensities(userDensity);
        List<EntityMasterEntity> entities = activeEngine().searchByKeyword(tenantId, densities, kw, RAG_LIMIT);
        Map<String, List<EntityLabelEntity>> labelsByEntity = batchLabels(entities);
        Map<String, List<EntityMappingEntity>> mappingsByEntity = batchMappings(entities);
        return entities.stream().map(e -> {
            List<String> labelNames = labelsByEntity.getOrDefault(e.getId(), List.of()).stream()
                    .map(EntityLabelEntity::getLabelName).filter(n -> n != null).collect(Collectors.toList());
            List<String> sources = mappingsByEntity.getOrDefault(e.getId(), List.of()).stream()
                    .map(EntityMappingEntity::getSourceSystem).distinct().collect(Collectors.toList());
            return "实体[" + e.getEntityType() + "]" + (e.getDisplayName() != null ? e.getDisplayName() : "")
                    + "（主标识已脱敏）标签:" + String.join("、", labelNames)
                    + " 来源:" + String.join("、", sources);
        }).collect(Collectors.joining("\n"));
    }

    private List<IntelligenceRes> toResList(List<EntityMasterEntity> entities) {
        if (entities.isEmpty()) return List.of();
        Map<String, List<EntityLabelEntity>> labelsByEntity = batchLabels(entities);
        Map<String, List<EntityMappingEntity>> mappingsByEntity = batchMappings(entities);
        return entities.stream().map(e -> toRes(e, labelsByEntity.getOrDefault(e.getId(), List.of()),
                mappingsByEntity.getOrDefault(e.getId(), List.of()))).collect(Collectors.toList());
    }

    private Map<String, List<EntityLabelEntity>> batchLabels(List<EntityMasterEntity> entities) {
        Set<String> ids = entities.stream().map(EntityMasterEntity::getId).collect(Collectors.toSet());
        return labelRepository.findByEntityIdIn(ids).stream()
                .collect(Collectors.groupingBy(EntityLabelEntity::getEntityId));
    }

    private Map<String, List<EntityMappingEntity>> batchMappings(List<EntityMasterEntity> entities) {
        Set<String> ids = entities.stream().map(EntityMasterEntity::getId).collect(Collectors.toSet());
        return mappingRepository.findByEntityIdIn(ids).stream()
                .collect(Collectors.groupingBy(EntityMappingEntity::getEntityId));
    }

    private IntelligenceRes toRes(EntityMasterEntity e, List<EntityLabelEntity> labels, List<EntityMappingEntity> mappings) {
        IntelligenceRes res = new IntelligenceRes();
        res.setEntityId(e.getId());
        res.setEntityType(e.getEntityType());
        res.setDisplayName(e.getDisplayName());
        res.setPrimaryKey(com.temporedata.guard.common.util.SensitiveUtil.maskKey(e.getPrimaryKey()));
        res.setDensity(e.getDensity());
        res.setLabels(labels.stream().map(l -> {
            IntelligenceRes.EntityLabelRes lr = new IntelligenceRes.EntityLabelRes();
            lr.setLabelType(l.getLabelType());
            lr.setLabelCode(l.getLabelCode());
            lr.setLabelName(l.getLabelName());
            lr.setSource(l.getSource());
            return lr;
        }).collect(Collectors.toList()));
        res.setSourceSystems(mappings.stream()
                .map(EntityMappingEntity::getSourceSystem).distinct().collect(Collectors.toList()));
        return res;
    }


}
