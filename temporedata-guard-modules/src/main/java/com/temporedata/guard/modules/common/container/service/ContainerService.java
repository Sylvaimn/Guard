package com.temporedata.guard.modules.common.container.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.common.container.entity.ContainerEntity;
import com.temporedata.guard.modules.common.container.repository.ContainerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Container management service.
 * Supports Docker/K8s container lifecycle management.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContainerService {

    private final ContainerRepository containerRepository;

    public List<ContainerEntity> listContainers(String tenantId) {
        return containerRepository.findByTenantId(tenantId);
    }

    public List<ContainerEntity> listByCluster(String clusterId) {
        return containerRepository.findByClusterId(clusterId);
    }

    public ContainerEntity getContainer(String containerId) {
        return containerRepository.findById(containerId)
                .orElseThrow(() -> new BusinessException("容器不存在"));
    }

    @Transactional
    public ContainerEntity createContainer(ContainerEntity container) {
        container.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 32));
        container.setStatus("CREATED");
        container.setCreateDateTime(LocalDateTime.now());
        container.setLastModifiedDateTime(LocalDateTime.now());
        return containerRepository.save(container);
    }

    @Transactional
    public ContainerEntity startContainer(String containerId) {
        ContainerEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new BusinessException("容器不存在"));
        if ("RUNNING".equals(container.getStatus())) {
            throw new BusinessException("容器已在运行中");
        }
        try {
            // In production, use Docker/K8s API to start container
            container.setStatus("RUNNING");
            container.setLastModifiedDateTime(LocalDateTime.now());
            log.info("Container started: {}", container.getName());
        } catch (Exception e) {
            container.setStatus("FAILED");
            container.setLog(e.getMessage());
        }
        return containerRepository.save(container);
    }

    @Transactional
    public ContainerEntity stopContainer(String containerId) {
        ContainerEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new BusinessException("容器不存在"));
        if (!"RUNNING".equals(container.getStatus())) {
            throw new BusinessException("容器未在运行中");
        }
        try {
            container.setStatus("STOPPED");
            container.setLastModifiedDateTime(LocalDateTime.now());
            log.info("Container stopped: {}", container.getName());
        } catch (Exception e) {
            container.setStatus("FAILED");
            container.setLog(e.getMessage());
        }
        return containerRepository.save(container);
    }

    @Transactional
    public void deleteContainer(String containerId) {
        ContainerEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new BusinessException("容器不存在"));
        if ("RUNNING".equals(container.getStatus())) {
            throw new BusinessException("运行中的容器不能删除，请先停止");
        }
        containerRepository.delete(container);
    }

    @Transactional
    public ContainerEntity updateContainer(String containerId, ContainerEntity updates) {
        ContainerEntity container = containerRepository.findById(containerId)
                .orElseThrow(() -> new BusinessException("容器不存在"));
        if (updates.getName() != null) container.setName(updates.getName());
        if (updates.getImage() != null) container.setImage(updates.getImage());
        if (updates.getCpuCores() != null) container.setCpuCores(updates.getCpuCores());
        if (updates.getMemoryMb() != null) container.setMemoryMb(updates.getMemoryMb());
        if (updates.getEnvVars() != null) container.setEnvVars(updates.getEnvVars());
        if (updates.getPortMappings() != null) container.setPortMappings(updates.getPortMappings());
        if (updates.getVolumeMounts() != null) container.setVolumeMounts(updates.getVolumeMounts());
        if (updates.getCommand() != null) container.setCommand(updates.getCommand());
        container.setLastModifiedDateTime(LocalDateTime.now());
        return containerRepository.save(container);
    }
}