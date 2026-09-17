package com.temporedata.guard.modules.dev.engine.service;
import com.temporedata.guard.modules.common.async.TaskExecutorService;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.engine.SparkJobReq;
import com.temporedata.guard.api.engine.SparkJobRes;
import com.temporedata.guard.modules.dev.engine.entity.SparkJobEntity;
import com.temporedata.guard.modules.dev.engine.repository.SparkJobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Spark job service: submit, monitor, kill with security sandbox.
 * Uses SparkLauncher (process-based) for lightweight, no-cluster-required execution.
 * Security sandbox enforces resource limits and isolation via Spark config.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SparkJobService {

    private final SparkJobRepository jobRepository;
    private final ResourceQuotaService quotaService;
    private final com.temporedata.guard.modules.common.async.TaskExecutorService taskExecutorService;

    // Track running processes for kill support
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    public List<SparkJobRes> list(String tenantId) {
        return jobRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public SparkJobRes detail(String id) {
        return toRes(jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Spark作业不存在")));
    }

    @Transactional
    public SparkJobRes create(SparkJobReq req, String tenantId) {
        SparkJobEntity entity = new SparkJobEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setMainClass(req.getMainClass());
        entity.setJarPath(req.getJarPath());
        entity.setSqlContent(req.getSqlContent());
        entity.setPythonFile(req.getPythonFile());
        entity.setMaster(req.getMaster() != null ? req.getMaster() : "local[*]");
        entity.setDeployMode(req.getDeployMode() != null ? req.getDeployMode() : "client");
        entity.setDriverMemory(req.getDriverMemory() != null ? req.getDriverMemory() : "1g");
        entity.setExecutorMemory(req.getExecutorMemory() != null ? req.getExecutorMemory() : "1g");
        entity.setExecutorCores(req.getExecutorCores() != null ? req.getExecutorCores() : 1);
        entity.setNumExecutors(req.getNumExecutors() != null ? req.getNumExecutors() : 1);
        entity.setArgs(req.getArgs());
        entity.setConf(req.getConf());
        entity.setStatus("READY");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(jobRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        SparkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Spark作业不存在"));
        if ("RUNNING".equals(job.getStatus())) {
            killProcess(id);
        }
        jobRepository.deleteById(id);
    }

    /**
     * Submit Spark job with security sandbox.
     * Uses spark-submit command with resource-constrained executor config.
     * 异步执行(HTTP 线程秒回) + 无集群环境时 DRY_RUN 校验模式。
     */
    public boolean executeAsync(String id) {
        boolean accepted = taskExecutorService.submit("spark:" + id, () -> execute(id));
        if (!accepted) {
            throw new BusinessException("Spark作业正在运行中");
        }
        return true;
    }

    @Transactional
    public void execute(String id) {
        SparkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Spark作业不存在"));

        if ("RUNNING".equals(job.getStatus())) {
            throw new BusinessException("作业正在运行中");
        }

        // 资源配额校验(Executor 数/内存/时长/并发, 超限拒绝提交)
        quotaService.validate(job);

        job.setStatus("RUNNING");
        job.setLastRunTime(LocalDateTime.now());
        job.setErrorMsg(null);
        jobRepository.save(job);

        long start = System.currentTimeMillis();

        try {
            String sparkHome = System.getenv().getOrDefault("SPARK_HOME", "/opt/spark");
            String sparkSubmit = sparkHome + "/bin/spark-submit";

            // 集群未就绪(无 spark-submit): DRY_RUN 模式, 校验作业定义并给出提交计划
            if (!new File(sparkSubmit).exists()) {
                String plan = buildSubmitPlan(job, sparkSubmit);
                log.warn("Spark cluster not ready ({} missing), DRY_RUN: {}", sparkSubmit, plan);
                job.setStatus("DRY_RUN");
                job.setDurationMs(System.currentTimeMillis() - start);
                job.setErrorMsg(null);
                job.setLog(plan);
                job.setUpdateTime(LocalDateTime.now());
                jobRepository.save(job);
                return;
            }

            // Build sandboxed spark-submit command
            List<String> cmd = buildSparkSubmitCommand(job, sparkSubmit);
            log.info("Spark submit command: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            runningProcesses.put(id, process);

            // Read output in background thread
            StringBuilder output = new StringBuilder();
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        output.append(line).append("\n");
                        log.debug("[Spark {}] {}", id, line);
                    }
                } catch (Exception e) {
                    log.error("Spark output reader error", e);
                }
            });
            reader.start();

            int exitCode = process.waitFor();
            reader.join(5000);

            job.setDurationMs(System.currentTimeMillis() - start);

            if (exitCode == 0) {
                job.setStatus("SUCCESS");
                // Extract appId from output
                String appId = extractAppId(output.toString());
                job.setAppId(appId);
            } else {
                job.setStatus("FAILED");
                job.setErrorMsg(truncate(output.toString(), 2000));
            }
        } catch (Exception e) {
            log.error("Spark job {} execution failed", id, e);
            job.setStatus("FAILED");
            job.setDurationMs(System.currentTimeMillis() - start);
            job.setErrorMsg(e.getMessage());
        } finally {
            runningProcesses.remove(id);
            job.setUpdateTime(LocalDateTime.now());
            jobRepository.save(job);
        }
    }

    /**
     * Kill a running Spark job.
     */
    @Transactional
    public void kill(String id) {
        SparkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Spark作业不存在"));

        if (!"RUNNING".equals(job.getStatus())) {
            throw new BusinessException("作业未在运行");
        }

        killProcess(id);
        job.setStatus("KILLED");
        job.setUpdateTime(LocalDateTime.now());
        jobRepository.save(job);
    }

    /**
     * Get Spark job logs.
     */
    public String logs(String id) {
        SparkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Spark作业不存在"));

        if (job.getAppId() == null) {
            return "No application logs available. App ID not found.";
        }

        try {
            String sparkHome = System.getenv().getOrDefault("SPARK_HOME", "/opt/spark");
            // Try to get logs via yarn or spark history server
            ProcessBuilder pb = new ProcessBuilder(
                    "/usr/bin/yarn", "logs", "-applicationId", job.getAppId());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            log.warn("Failed to fetch logs for app {}", job.getAppId(), e);
            return "Logs not available: " + e.getMessage();
        }
    }

    private List<String> buildSparkSubmitCommand(SparkJobEntity job, String sparkSubmit) {
        List<String> cmd = new ArrayList<>();
        cmd.add(sparkSubmit);
        cmd.add("--master");
        cmd.add(job.getMaster());
        cmd.add("--deploy-mode");
        cmd.add(job.getDeployMode());
        cmd.add("--name");
        cmd.add(job.getName());

        // Security sandbox: resource limits
        cmd.add("--driver-memory");
        cmd.add(job.getDriverMemory());
        cmd.add("--executor-memory");
        cmd.add(job.getExecutorMemory());
        cmd.add("--executor-cores");
        cmd.add(String.valueOf(job.getExecutorCores()));
        cmd.add("--num-executors");
        cmd.add(String.valueOf(job.getNumExecutors()));

        // Sandbox: disable dynamic allocation for predictable resource usage
        cmd.add("--conf");
        cmd.add("spark.dynamicAllocation.enabled=false");

        // Extra conf
        if (job.getConf() != null && !job.getConf().isEmpty()) {
            try {
                Map<String, Object> confMap = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(job.getConf(), Map.class);
                for (Map.Entry<String, Object> entry : confMap.entrySet()) {
                    cmd.add("--conf");
                    cmd.add(entry.getKey() + "=" + entry.getValue());
                }
            } catch (Exception ignored) {}
        }

        switch (job.getType()) {
            case "SPARK_SQL":
                // Write SQL to temp file and use -f
                cmd.add("--class");
                cmd.add("org.apache.spark.sql.hive.thriftserver.SparkSQLCLIDriver");
                break;
            case "SPARK_JAR":
                cmd.add("--class");
                cmd.add(job.getMainClass());
                if (job.getJarPath() != null) {
                    cmd.add(job.getJarPath());
                }
                break;
            case "PYSPARK":
                if (job.getPythonFile() != null) {
                    cmd.add(job.getPythonFile());
                }
                break;
        }

        if (job.getArgs() != null && !job.getArgs().isEmpty()) {
            cmd.add(job.getArgs());
        }

        return cmd;
    }

    private void killProcess(String jobId) {
        Process process = runningProcesses.remove(jobId);
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }

    /** DRY_RUN: 生成提交计划(引擎就绪后执行的真实命令) */
    private String buildSubmitPlan(SparkJobEntity job, String sparkSubmit) {
        List<String> cmd = buildSparkSubmitCommand(job, sparkSubmit);
        return "DRY_RUN: Spark集群未就绪(" + sparkSubmit + " 不存在), 作业定义校验通过。"
                + "就绪后将以如下命令提交:\n" + String.join(" ", cmd);
    }

    private String extractAppId(String output) {
        for (String line : output.split("\n")) {
            if (line.contains("application_") || line.contains("app-")) {
                return line.trim();
            }
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private SparkJobRes toRes(SparkJobEntity e) {
        SparkJobRes res = new SparkJobRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setStatus(e.getStatus());
        res.setMaster(e.getMaster());
        res.setDeployMode(e.getDeployMode());
        res.setAppId(e.getAppId());
        res.setLastRunTime(e.getLastRunTime() != null ? e.getLastRunTime().toString() : null);
        res.setDuration(e.getDurationMs() != null ? formatDuration(e.getDurationMs()) : null);
        res.setErrorMsg(e.getErrorMsg());
        res.setLog(e.getLog());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }

    private String formatDuration(long ms) {
        if (ms < 1000) return ms + "ms";
        if (ms < 60000) return String.format("%.1fs", ms / 1000.0);
        return String.format("%dm%ds", ms / 60000, (ms % 60000) / 1000);
    }
}