package com.temporedata.guard.modules.police.intelligence.service;

import com.temporedata.guard.modules.police.entitymaster.entity.EntityMasterEntity;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityMasterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 数据库检索引擎(默认): JPQL 下推 租户/密级/关键字。
 */
@Component
@RequiredArgsConstructor
public class DatabaseSearchEngine implements SearchEngine {

    private final EntityMasterRepository masterRepository;

    @Override
    public String name() {
        return "database";
    }

    @Override
    public List<EntityMasterEntity> searchByKeyword(String tenantId, List<String> densities, String keyword, int limit) {
        return masterRepository.searchByKeyword(tenantId, densities, keyword.toLowerCase(),
                PageRequest.of(0, limit));
    }

    @Override
    public List<EntityMasterEntity> searchByLabelCode(String tenantId, List<String> densities, String labelCode, int limit) {
        return masterRepository.searchByLabelCode(tenantId, densities, labelCode, PageRequest.of(0, limit));
    }
}
