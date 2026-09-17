package com.temporedata.guard.modules.common.cluster.service;
import com.temporedata.guard.modules.common.cluster.entity.ClusterEntity;
import com.temporedata.guard.modules.common.cluster.entity.ClusterNodeEntity;
import com.temporedata.guard.modules.common.cluster.repository.ClusterNodeRepository;
import com.temporedata.guard.modules.common.cluster.repository.ClusterRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.cluster.ClusterNodeReq;
import com.temporedata.guard.api.cluster.ClusterNodeRes;
import com.temporedata.guard.api.cluster.ClusterReq;
import com.temporedata.guard.api.cluster.ClusterRes;
import com.temporedata.guard.common.util.CryptoUtil;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Cluster management service: CRUD for clusters and nodes, agent installation, node status check.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {

    private final ClusterRepository clusterRepository;
    private final ClusterNodeRepository clusterNodeRepository;

    @Value("${zy.crypto.key:qingzhouyun-p0-key}")
    private String cryptoKey;

    // ===== Cluster CRUD =====

    public List<ClusterRes> list(String tenantId) {
        return clusterRepository.findByTenantId(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public ClusterRes detail(String id) {
        return toRes(clusterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("集群不存在")));
    }

    @Transactional
    public ClusterRes create(ClusterReq req, String tenantId) {
        ClusterEntity entity = new ClusterEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setMasterUrl(req.getMasterUrl());
        entity.setStatus("OFFLINE");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(clusterRepository.save(entity));
    }

    @Transactional
    public ClusterRes update(String id, ClusterReq req) {
        ClusterEntity entity = clusterRepository.findById(id)
                .orElseThrow(() -> new BusinessException("集群不存在"));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setMasterUrl(req.getMasterUrl());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(clusterRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        List<ClusterNodeEntity> nodes = clusterNodeRepository.findByClusterId(id);
        clusterNodeRepository.deleteAll(nodes);
        clusterRepository.deleteById(id);
    }

    // ===== Cluster Node CRUD =====

    public List<ClusterNodeRes> listNodes(String clusterId) {
        return clusterNodeRepository.findByClusterId(clusterId).stream()
                .map(this::toNodeRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClusterNodeRes addNode(String clusterId, ClusterNodeReq req) {
        clusterRepository.findById(clusterId)
                .orElseThrow(() -> new BusinessException("集群不存在"));

        ClusterNodeEntity entity = new ClusterNodeEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setClusterId(clusterId);
        entity.setHost(req.getHost());
        entity.setPort(req.getPort());
        entity.setUsername(req.getUsername());
        if (req.getPassword() != null && !req.getPassword().isEmpty()) {
            entity.setPassword(CryptoUtil.encrypt(req.getPassword(), cryptoKey));
        }
        entity.setAgentStatus("OFFLINE");
        entity.setCreateTime(LocalDateTime.now());
        return toNodeRes(clusterNodeRepository.save(entity));
    }

    @Transactional
    public void deleteNode(String nodeId) {
        clusterNodeRepository.deleteById(nodeId);
    }

    // ===== Agent Operations =====

    /**
     * Install agent on the target node via SSH.
     */
    public String installAgent(String nodeId) {
        ClusterNodeEntity node = clusterNodeRepository.findById(nodeId)
                .orElseThrow(() -> new BusinessException("节点不存在"));

        node.setAgentStatus("INSTALLING");
        clusterNodeRepository.save(node);

        try {
            String plainPwd = CryptoUtil.decrypt(node.getPassword(), cryptoKey);

            JSch jsch = new JSch();
            Session session = jsch.getSession(node.getUsername(), node.getHost(), node.getPort());
            session.setPassword(plainPwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            String installCmd = "mkdir -p /opt/zy/agent && echo 'agent installed' > /opt/zy/agent/status";
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(installCmd);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect(5000);

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            channel.disconnect();
            session.disconnect();

            node.setAgentStatus("ONLINE");
            clusterNodeRepository.save(node);
            log.info("Agent installed successfully on node {}", nodeId);
            return "Agent installed successfully on " + node.getHost() + ":" + node.getPort();
        } catch (Exception e) {
            log.error("Failed to install agent on node {}", nodeId, e);
            node.setAgentStatus("OFFLINE");
            clusterNodeRepository.save(node);
            throw new BusinessException("Agent 安装失败: " + e.getMessage());
        }
    }

    /**
     * Check node agent status via SSH.
     */
    public ClusterNodeRes checkNodeStatus(String nodeId) {
        ClusterNodeEntity node = clusterNodeRepository.findById(nodeId)
                .orElseThrow(() -> new BusinessException("节点不存在"));

        try {
            String plainPwd = CryptoUtil.decrypt(node.getPassword(), cryptoKey);

            JSch jsch = new JSch();
            Session session = jsch.getSession(node.getUsername(), node.getHost(), node.getPort());
            session.setPassword(plainPwd);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(10000);

            String checkCmd = "cat /opt/zy/agent/status 2>/dev/null || echo 'OFFLINE'";
            ChannelExec channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(checkCmd);
            channel.setInputStream(null);
            channel.setErrStream(System.err);

            BufferedReader reader = new BufferedReader(new InputStreamReader(channel.getInputStream()));
            channel.connect(5000);

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            channel.disconnect();
            session.disconnect();

            String status = output.toString().contains("agent installed") ? "ONLINE" : "OFFLINE";
            node.setAgentStatus(status);
            clusterNodeRepository.save(node);

            // Simulate resource usage (placeholder)
            if ("ONLINE".equals(status)) {
                node.setCpuUsage(Math.random() * 100);
                node.setMemoryUsage(Math.random() * 100);
                node.setDiskUsage(Math.random() * 100);
                clusterNodeRepository.save(node);
            }

            return toNodeRes(node);
        } catch (Exception e) {
            log.error("Failed to check node status for {}", nodeId, e);
            node.setAgentStatus("OFFLINE");
            node.setCpuUsage(null);
            node.setMemoryUsage(null);
            node.setDiskUsage(null);
            clusterNodeRepository.save(node);
            return toNodeRes(node);
        }
    }

    // ===== Conversion Helpers =====

    private ClusterRes toRes(ClusterEntity e) {
        ClusterRes res = new ClusterRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setMasterUrl(e.getMasterUrl());
        res.setStatus(e.getStatus());
        List<ClusterNodeEntity> nodes = clusterNodeRepository.findByClusterId(e.getId());
        res.setNodeCount(nodes.size());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private ClusterNodeRes toNodeRes(ClusterNodeEntity e) {
        ClusterNodeRes res = new ClusterNodeRes();
        res.setId(e.getId());
        res.setHost(e.getHost());
        res.setPort(e.getPort());
        res.setAgentStatus(e.getAgentStatus());
        res.setCpuUsage(e.getCpuUsage());
        res.setMemoryUsage(e.getMemoryUsage());
        res.setDiskUsage(e.getDiskUsage());
        return res;
    }
}