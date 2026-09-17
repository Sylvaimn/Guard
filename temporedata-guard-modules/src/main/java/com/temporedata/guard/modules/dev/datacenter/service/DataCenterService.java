package com.temporedata.guard.modules.dev.datacenter.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.datacenter.DataCenterOverviewRes;
import com.temporedata.guard.api.datacenter.DataCenterRes;
import com.temporedata.guard.modules.dev.datacenter.entity.DataCenterEntity;
import com.temporedata.guard.modules.dev.datacenter.repository.DataCenterRepository;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository;
import com.temporedata.guard.modules.dev.quality.repository.QualityRuleRepository;
import com.temporedata.guard.modules.data.service.repository.DataApiRepository;
import com.temporedata.guard.modules.dev.sync.repository.SyncTaskRepository;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data center service: dashboard CRUD and overview statistics.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCenterService {

    private final DataCenterRepository dataCenterRepository;
    private final DatasourceRepository datasourceRepository;
    private final TableMetadataRepository tableMetadataRepository;
    private final WorkflowRepository workflowRepository;
    private final DataApiRepository dataApiRepository;
    private final SyncTaskRepository syncTaskRepository;
    private final QualityRuleRepository qualityRuleRepository;

    public List<DataCenterRes> listAll() {
        return dataCenterRepository.findAll().stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public List<DataCenterRes> listByCategory(String category) {
        return dataCenterRepository.findByCategory(category).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public DataCenterRes create(DataCenterEntity entity) {
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setStatus("ACTIVE");
        if (entity.getRefreshInterval() == null) {
            entity.setRefreshInterval(60);
        }
        entity.setCreateTime(LocalDateTime.now());
        return toRes(dataCenterRepository.save(entity));
    }

    @Transactional
    public DataCenterRes update(String id, DataCenterEntity entity) {
        DataCenterEntity existing = dataCenterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Data center item not found"));
        existing.setName(entity.getName());
        existing.setDescription(entity.getDescription());
        existing.setCategory(entity.getCategory());
        existing.setConfig(entity.getConfig());
        existing.setDatasourceId(entity.getDatasourceId());
        if (entity.getRefreshInterval() != null) {
            existing.setRefreshInterval(entity.getRefreshInterval());
        }
        existing.setUpdateTime(LocalDateTime.now());
        return toRes(dataCenterRepository.save(existing));
    }

    @Transactional
    public void delete(String id) {
        dataCenterRepository.deleteById(id);
    }

    public DataCenterOverviewRes getOverview() {
        DataCenterOverviewRes overview = new DataCenterOverviewRes();
        overview.setDatasourceCount(datasourceRepository.count());
        overview.setTableCount(tableMetadataRepository.count());
        overview.setWorkflowCount(workflowRepository.count());
        overview.setApiCount(dataApiRepository.count());
        overview.setSyncTaskCount(syncTaskRepository.count());
        overview.setQualityRuleCount(qualityRuleRepository.count());
        return overview;
    }

    private DataCenterRes toRes(DataCenterEntity e) {
        DataCenterRes res = new DataCenterRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setDescription(e.getDescription());
        res.setCategory(e.getCategory());
        res.setConfig(e.getConfig());
        res.setDatasourceId(e.getDatasourceId());
        res.setRefreshInterval(e.getRefreshInterval());
        res.setStatus(e.getStatus());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }
}