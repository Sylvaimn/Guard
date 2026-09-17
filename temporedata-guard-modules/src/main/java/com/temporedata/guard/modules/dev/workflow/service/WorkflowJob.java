package com.temporedata.guard.modules.dev.workflow.service;

import com.temporedata.guard.modules.dev.workflow.entity.WorkflowExecuteEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Quartz Job that executes a workflow.
 * Each job execution creates a new WorkflowExecuteEntity and runs the workflow asynchronously.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowJob implements Job {

    private final WorkflowExecutor workflowExecutor;

    @Override
    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        String workflowId = dataMap.getString("workflowId");

        log.info("Quartz triggered workflow execution: {}", workflowId);

        WorkflowExecuteEntity execution = new WorkflowExecuteEntity();
        execution.setId(UUID.randomUUID().toString().replace("-", ""));
        execution.setWorkflowId(workflowId);
        execution.setStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        execution.setLog("Quartz scheduled execution");

        workflowExecutor.executeAsync(execution.getId());
    }
}