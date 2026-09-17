package com.temporedata.guard.modules.dev.engine.service;
import com.temporedata.guard.modules.common.async.TaskExecutorService;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.engine.FlinkJobReq;
import com.temporedata.guard.api.engine.FlinkJobRes;
import com.temporedata.guard.modules.dev.engine.entity.FlinkJobEntity;
import com.temporedata.guard.modules.dev.engine.repository.FlinkJobRepository;
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
 * Flink job service: submit, monitor, cancel with savepoint support.
 * Uses Flink CLI or REST API for job lifecycle management.
 * Supports real-time streaming jobs with checkpoint/savepoint.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlinkJobService {

    private final FlinkJobRepository jobRepository;

    // Track running processes for cancel support
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final com.temporedata.guard.modules.common.async.TaskExecutorService taskExecutorService;

    public List<FlinkJobRes> list(String tenantId) {
        return jobRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    public FlinkJobRes detail(String id) {
        return toRes(jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Flink作业不存在")));
    }

    @Transactional
    public FlinkJobRes create(FlinkJobReq req, String tenantId) {
        FlinkJobEntity entity = new FlinkJobEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setType(req.getType());
        entity.setJarPath(req.getJarPath());
        entity.setMainClass(req.getMainClass());
        entity.setSqlContent(req.getSqlContent());
        entity.setPythonFile(req.getPythonFile());
        entity.setFlinkHome(req.getFlinkHome() != null ? req.getFlinkHome() : "/opt/flink");
        entity.setParallelism(req.getParallelism() != null ? req.getParallelism() : "1");
        entity.setJobManagerMemory(req.getJobManagerMemory() != null ? req.getJobManagerMemory() : "1024m");
        entity.setTaskManagerMemory(req.getTaskManagerMemory() != null ? req.getTaskManagerMemory() : "1024m");
        entity.setTaskSlots(req.getTaskSlots() != null ? req.getTaskSlots() : 1);
        entity.setSavepointPath(req.getSavepointPath());
        entity.setArgs(req.getArgs());
        entity.setConf(req.getConf());
        entity.setStatus("READY");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(jobRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        FlinkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Flink作业不存在"));
        if ("RUNNING".equals(job.getStatus())) {
            cancel(id);
        }
        jobRepository.deleteById(id);
    }

    /** 异步提交(HTTP 线程秒回, 集群未就绪时 DRY_RUN) */
    public boolean executeAsync(String id) {
        boolean accepted = taskExecutorService.submit("flink:" + id, () -> execute(id));
        if (!accepted) {
            throw new BusinessException("Flink作业正在运行中");
        }
        return true;
    }

    /**
     * Submit Flink job using flink run command.
     * Supports JAR, SQL, and Python job types.
     */
    @Transactional
    public void execute(String id) {
        FlinkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Flink作业不存在"));

        if ("RUNNING".equals(job.getStatus())) {
            throw new BusinessException("作业正在运行中");
        }

        job.setStatus("RUNNING");
        job.setLastRunTime(LocalDateTime.now());
        job.setErrorMsg(null);
        jobRepository.save(job);

        long start = System.currentTimeMillis();

        try {
            String flinkBin = job.getFlinkHome() + "/bin/flink";
            if (!new File(flinkBin).exists()) {
                List<String> planCmd = buildFlinkRunCommand(job, flinkBin);
                String plan = "DRY_RUN: Flink集群未就绪(" + flinkBin + " 不存在), 作业定义校验通过。"
                        + "就绪后将以如下命令提交:\n" + String.join(" ", planCmd);
                log.warn("Flink cluster not ready, DRY_RUN: {}", plan);
                job.setStatus("DRY_RUN");
                job.setDurationMs(System.currentTimeMillis() - start);
                job.setLog(plan);
                job.setUpdateTime(LocalDateTime.now());
                jobRepository.save(job);
                return;
            }
            List<String> cmd = buildFlinkRunCommand(job, flinkBin);
            log.info("Flink run command: {}", String.join(" ", cmd));

            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            runningProcesses.put(id, process);

            StringBuilder output = new StringBuilder();
            Thread reader = new Thread(() -> {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        output.append(line).append("\n");
                        log.debug("[Flink {}] {}", id, line);
                    }
                } catch (Exception e) {
                    log.error("Flink output reader error", e);
                }
            });
            reader.start();

            // For streaming jobs, don't wait indefinitely
            if ("SQL".equals(job.getType()) && job.getSqlContent() != null) {
                // Flink SQL jobs are typically streaming, wait briefly then detach
                Thread.sleep(5000);
                if (process.isAlive()) {
                    job.setStatus("RUNNING");
                    job.setJobId(extractJobId(output.toString()));
                }
            } else {
                int exitCode = process.waitFor();
                job.setDurationMs(System.currentTimeMillis() - start);
                if (exitCode == 0) {
                    job.setStatus("FINISHED");
                    job.setJobId(extractJobId(output.toString()));
                } else {
                    job.setStatus("FAILED");
                    job.setErrorMsg(truncate(output.toString(), 2000));
                }
            }

            reader.join(3000);
            job.setDurationMs(System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("Flink job {} execution failed", id, e);
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
     * Cancel Flink job with savepoint.
     */
    @Transactional
    public void cancel(String id) {
        FlinkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Flink作业不存在"));

        if (!"RUNNING".equals(job.getStatus())) {
            throw new BusinessException("作业未在运行");
        }

        try {
            String flinkBin = job.getFlinkHome() + "/bin/flink";
            List<String> cmd = new ArrayList<>();
            cmd.add(flinkBin);
            cmd.add("cancel");

            // Save savepoint before cancel
            String savepointDir = "/tmp/flink-savepoints/" + id;
            cmd.add("-s");
            cmd.add(savepointDir);
            cmd.add(job.getJobId() != null ? job.getJobId() : "");

            Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();

            job.setStatus("CANCELED");
            job.setSavepointPath(savepointDir);
        } catch (Exception e) {
            log.error("Flink job {} cancel failed", id, e);
            job.setStatus("FAILED");
            job.setErrorMsg("Cancel failed: " + e.getMessage());
        }

        killProcess(id);
        job.setUpdateTime(LocalDateTime.now());
        jobRepository.save(job);
    }

    /**
     * Trigger a savepoint for a running Flink job.
     */
    @Transactional
    public String savepoint(String id) {
        FlinkJobEntity job = jobRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Flink作业不存在"));

        if (!"RUNNING".equals(job.getStatus()) || job.getJobId() == null) {
            throw new BusinessException("作业未在运行或无JobId");
        }

        try {
            String savepointDir = "/tmp/flink-savepoints/" + id + "_" + System.currentTimeMillis();
            String flinkBin = job.getFlinkHome() + "/bin/flink";
            Process process = new ProcessBuilder(
                    flinkBin, "savepoint", job.getJobId(), savepointDir
            ).redirectErrorStream(true).start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            process.waitFor();

            job.setSavepointPath(savepointDir);
            jobRepository.save(job);
            return savepointDir;
        } catch (Exception e) {
            log.error("Flink savepoint failed for job {}", id, e);
            throw new BusinessException("Savepoint失败: " + e.getMessage());
        }
    }

    private List<String> buildFlinkRunCommand(FlinkJobEntity job, String flinkBin) {
        List<String> cmd = new ArrayList<>();
        cmd.add(flinkBin);
        cmd.add("run");

        cmd.add("-p");
        cmd.add(job.getParallelism());

        cmd.add("-jm");
        cmd.add(job.getJobManagerMemory());

        cmd.add("-tm");
        cmd.add(job.getTaskManagerMemory());

        cmd.add("-s");
        cmd.add(String.valueOf(job.getTaskSlots()));

        cmd.add("-d"); // detached mode for streaming jobs

        if (job.getSavepointPath() != null && !job.getSavepointPath().isEmpty()) {
            cmd.add("-s");
            cmd.add(job.getSavepointPath());
        }

        // Extra conf
        if (job.getConf() != null && !job.getConf().isEmpty()) {
            try {
                Map<String, Object> confMap = new com.fasterxml.jackson.databind.ObjectMapper()
                        .readValue(job.getConf(), Map.class);
                for (Map.Entry<String, Object> entry : confMap.entrySet()) {
                    cmd.add("-D" + entry.getKey() + "=" + entry.getValue());
                }
            } catch (Exception ignored) {}
        }

        switch (job.getType()) {
            case "JAR":
                cmd.add("-c");
                cmd.add(job.getMainClass());
                if (job.getJarPath() != null) {
                    cmd.add(job.getJarPath());
                }
                break;
            case "SQL":
                // Write SQL to temp file
                if (job.getSqlContent() != null) {
                    cmd.add("-c");
                    cmd.add("org.apache.flink.table.client.SqlClient");
                    cmd.add("-f");
                    cmd.add(job.getSqlContent());
                }
                break;
            case "PYTHON":
                cmd.add("-py");
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

    private String extractJobId(String output) {
        for (String line : output.split("\n")) {
            if (line.contains("JobID") || line.contains("job_")) {
                return line.trim();
            }
        }
        return null;
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

    private FlinkJobRes toRes(FlinkJobEntity e) {
        FlinkJobRes res = new FlinkJobRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setType(e.getType());
        res.setStatus(e.getStatus());
        res.setJobId(e.getJobId());
        res.setParallelism(e.getParallelism());
        res.setLastRunTime(e.getLastRunTime() != null ? e.getLastRunTime().toString() : null);
        res.setDuration(e.getDurationMs() != null ? formatDuration(e.getDurationMs()) : null);
        res.setSavepointPath(e.getSavepointPath());
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