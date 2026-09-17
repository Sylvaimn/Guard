package com.temporedata.guard.modules.data.datasource.service;
import com.temporedata.guard.modules.data.datasource.plugin.PluginRegistry;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.datasource.DatasourceReq;
import com.temporedata.guard.api.datasource.DatasourceRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import com.temporedata.guard.modules.data.datasource.repository.DatasourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DatasourceService {

    private final DatasourceRepository datasourceRepository;

    private final JdbcConnectionManager connectionManager;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;
    private final com.temporedata.guard.modules.data.datasource.plugin.PluginRegistry pluginRegistry;

    public List<DatasourceRes> list(String tenantId) {
        return datasourceRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public DatasourceRes detail(String id) {
        return toRes(findById(id));
    }

    public DatasourceEntity findById(String id) {
        return datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
    }

    @Transactional
    public DatasourceRes create(DatasourceReq req, String tenantId) {
        DatasourceEntity entity = new DatasourceEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType().toUpperCase());
        entity.setHost(req.getHost());
        entity.setPort(req.getPort());
        entity.setDatabase(req.getDatabase());
        entity.setUsername(req.getUsername());
        entity.setPassword(CryptoUtil.encrypt(req.getPassword(), cryptoKey));
        entity.setParams(req.getParams());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(datasourceRepository.save(entity));
    }

    @Transactional
    public DatasourceRes update(String id, DatasourceReq req) {
        DatasourceEntity entity = datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        entity.setName(req.getName());
        entity.setType(req.getType().toUpperCase());
        entity.setHost(req.getHost());
        entity.setPort(req.getPort());
        entity.setDatabase(req.getDatabase());
        entity.setUsername(req.getUsername());
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            entity.setPassword(CryptoUtil.encrypt(req.getPassword(), cryptoKey));
        }
        entity.setParams(req.getParams());
        connectionManager.remove(id);
        return toRes(datasourceRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        connectionManager.remove(id);
        datasourceRepository.deleteById(id);
    }

    public boolean testConnection(String id) {
        DatasourceEntity entity = datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        return pluginRegistry.get(entity.getType()).test(entity);
    }

    /** 按插件元数据采集(湖仓快照/Kafka topic/ClickHouse 表/JDBC 表清单) */
    public java.util.List<java.util.Map<String, Object>> pluginMeta(String id) {
        DatasourceEntity entity = datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        return pluginRegistry.get(entity.getType()).meta(entity);
    }

    /** 已注册插件类型与能力 */
    public java.util.Map<String, String[]> pluginTypes() {
        return pluginRegistry.capabilities();
    }

    public String decryptPassword(String id) {
        DatasourceEntity entity = datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        return CryptoUtil.decrypt(entity.getPassword(), cryptoKey);
    }

    /**
     * Execute SQL on a datasource. Used for testing / initializing sample data.
     * Returns the number of affected rows.
     */
    public int executeSql(String id, String sql) {
        DatasourceEntity entity = datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        return pluginRegistry.get(entity.getType()).execute(entity, sql);
    }

    /**
     * Query a single value (for sanity check).
     */
    public java.util.List<java.util.Map<String, Object>> querySql(String id, String sql) {
        DatasourceEntity entity = datasourceRepository.findById(id)
                .orElseThrow(() -> new BusinessException("数据源不存在"));
        return pluginRegistry.get(entity.getType()).query(entity, sql);
    }

    private DatasourceRes toRes(DatasourceEntity e) {
        DatasourceRes res = new DatasourceRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setHost(e.getHost());
        res.setPort(e.getPort());
        res.setDatabase(e.getDatabase());
        res.setUsername(e.getUsername());
        res.setParams(e.getParams());
        res.setCreateTime(e.getCreateTime().toString());
        return res;
    }
}