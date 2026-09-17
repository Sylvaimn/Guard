package com.temporedata.guard.modules.dev.workflow.service;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowEdgeEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowEdgeRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowExecuteEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowExecuteRepository;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeEntity;
import com.temporedata.guard.modules.dev.workflow.entity.WorkflowNodeExecuteEntity;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowNodeExecuteRepository;
import com.temporedata.guard.modules.dev.workflow.repository.WorkflowNodeRepository;

import com.temporedata.guard.modules.dev.workflow.entity.*;
import com.temporedata.guard.modules.dev.workflow.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Priority-based workflow scheduler with configurable worker pool.
 * Implements the "Scheduler Core Layer" from the architecture:
 * - Priority queue for task ordering
 * - Worker pool for parallel execution
 * - State machine transitions
 */
@Slf4j
@Component
public class WorkflowScheduler {

    private final WorkflowExecuteRepository executeRepository;
    private final WorkflowNodeRepository nodeRepository;
    private final WorkflowEdgeRepository edgeRepository;
    private final WorkflowNodeExecuteRepository nodeExecuteRepository;
    private final WorkflowDagService dagService;
    private final WorkflowExecutor executor;

    // Priority queue (HIGHEST=0, HIGH=1, MEDIUM=2, LOW=3, LOWEST=4)
    private final PriorityBlockingQueue<QueuedExecution> taskQueue;
    private final ExecutorService workerPool;

    // Worker pool size (configurable)
    private static final int WORKER_POOL_SIZE = 4;
    private static final int QUEUE_CAPACITY = 100;

    public WorkflowScheduler(WorkflowExecuteRepository executeRepository,
                             WorkflowNodeRepository nodeRepository,
                             WorkflowEdgeRepository edgeRepository,
                             WorkflowNodeExecuteRepository nodeExecuteRepository,
                             WorkflowDagService dagService,
                             WorkflowExecutor executor) {
        this.executeRepository = executeRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.nodeExecuteRepository = nodeExecuteRepository;
        this.dagService = dagService;
        this.executor = executor;

        this.taskQueue = new PriorityBlockingQueue<>(QUEUE_CAPACITY,
                Comparator.comparingInt(QueuedExecution::getPriorityOrdinal));
        this.workerPool = Executors.newFixedThreadPool(WORKER_POOL_SIZE);
    }

    @PostConstruct
    public void startWorkers() {
        for (int i = 0; i < WORKER_POOL_SIZE; i++) {
            workerPool.submit(new Worker());
        }
        log.info("WorkflowScheduler started with {} workers", WORKER_POOL_SIZE);
    }

    @PreDestroy
    public void shutdown() {
        workerPool.shutdownNow();
        log.info("WorkflowScheduler shut down");
    }

    /**
     * Submit a workflow execution to the priority queue.
     */
    public void submit(String executionId, String priority) {
        WorkflowExecuteEntity execution = executeRepository.findById(executionId).orElse(null);
        if (execution == null) return;

        execution.setStatus("SUBMITTED");
        executeRepository.save(execution);

        int priorityOrdinal = mapPriority(priority);
        taskQueue.offer(new QueuedExecution(executionId, priorityOrdinal));

        log.info("Workflow {} submitted to queue (priority={})", executionId, priority);
    }

    /**
     * Get pending queue size.
     */
    public int getQueueSize() {
        return taskQueue.size();
    }

    /**
     * Worker thread that consumes from the priority queue.
     */
    private class Worker implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    QueuedExecution task = taskQueue.poll(5, TimeUnit.SECONDS);
                    if (task == null) continue;

                    executeTask(task.executionId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    /**
     * Execute a single workflow task.
     * Implements the state machine: SUBMITTED → PENDING → RUNNING → SUCCESS/FAILED
     */
    private void executeTask(String executionId) {
        WorkflowExecuteEntity execution = executeRepository.findById(executionId).orElse(null);
        if (execution == null) return;

        // Transition: SUBMITTED → PENDING → RUNNING
        execution.setStatus("PENDING");
        executeRepository.save(execution);

        try {
            String workflowId = execution.getWorkflowId();
            List<WorkflowNodeEntity> nodes = nodeRepository.findByWorkflowIdOrderBySortOrderAsc(workflowId);
            List<WorkflowEdgeEntity> edges = edgeRepository.findByWorkflowId(workflowId);

            dagService.validateDag(nodes, edges);
            List<WorkflowNodeEntity> sorted = dagService.topologicalSortNodes(nodes, edges);

            // Transition: PENDING → RUNNING
            execution.setStatus("RUNNING");
            executeRepository.save(execution);

            boolean allSuccess = true;
            Map<String, String> nodeStatus = new HashMap<>();
            StringBuilder logBuf = new StringBuilder();
            logBuf.append("=== Workflow Execution ===\n");

            for (WorkflowNodeEntity node : sorted) {
                // Check abort
                if (executor.isAborted(executionId)) {
                    execution.setStatus("ABORTED");
                    execution.setEndTime(LocalDateTime.now());
                    executeRepository.save(execution);
                    return;
                }

                // Conditional branching
                if (!executor.shouldExecuteNode(node, edges, nodeStatus)) {
                    WorkflowNodeExecuteEntity ne = executor.createNodeExecution(executionId, node);
                    ne.setStatus("SKIPPED");
                    ne.setDurationMs(0L);
                    ne.setEndTime(LocalDateTime.now());
                    nodeExecuteRepository.save(ne);
                    nodeStatus.put(node.getId(), "SKIPPED");
                    continue;
                }

                WorkflowNodeExecuteEntity ne = executor.createNodeExecution(executionId, node);
                nodeExecuteRepository.save(ne);

                long start = System.currentTimeMillis();
                boolean nodeSuccess = executor.executeNodeWithRetry(node, ne, logBuf);
                ne.setDurationMs(System.currentTimeMillis() - start);
                ne.setEndTime(LocalDateTime.now());
                nodeExecuteRepository.save(ne);

                String nodeResult = nodeSuccess ? "SUCCESS" : "FAILED";
                nodeStatus.put(node.getId(), nodeResult);
                if (!nodeSuccess) allSuccess = false;

                if (!nodeSuccess && "STOP".equals(node.getFailStrategy())) {
                    break;
                }
            }

            // Final state transition
            execution.setStatus(allSuccess ? "SUCCESS" : "FAILED");
            execution.setEndTime(LocalDateTime.now());
            execution.setLog(logBuf.toString());
            executeRepository.save(execution);

            log.info("Workflow execution {} completed: {}", executionId, execution.getStatus());

        } catch (Exception e) {
            log.error("Workflow execution {} failed: {}", executionId, e.getMessage());
            execution.setStatus("FAILED");
            execution.setEndTime(LocalDateTime.now());
            execution.setLog("Error: " + e.getMessage());
            executeRepository.save(execution);
        }
    }

    private int mapPriority(String priority) {
        if (priority == null) return 2;
        switch (priority.toUpperCase()) {
            case "HIGHEST": return 0;
            case "HIGH": return 1;
            case "MEDIUM": return 2;
            case "LOW": return 3;
            case "LOWEST": return 4;
            default: return 2;
        }
    }

    /**
     * Task entry in the priority queue.
     */
    private static class QueuedExecution {
        final String executionId;
        final int priorityOrdinal;

        QueuedExecution(String executionId, int priorityOrdinal) {
            this.executionId = executionId;
            this.priorityOrdinal = priorityOrdinal;
        }

        int getPriorityOrdinal() { return priorityOrdinal; }
    }
}