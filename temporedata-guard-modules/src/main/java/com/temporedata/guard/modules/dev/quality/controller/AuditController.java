package com.temporedata.guard.modules.dev.quality.controller;
import com.temporedata.guard.modules.security.security.audit.Audit;

import com.temporedata.guard.api.audit.AuditReportRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.quality.QualityCheckRes;
import com.temporedata.guard.modules.dev.quality.service.QualityService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import org.springframework.web.bind.annotation.*;


/**
 * Audit controller: batch quality checks, audit reports, issue tracking.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final QualityService qualityService;

    /**
     * Batch execute all quality rules for a datasource/table.
     */
    @PostMapping("/execute")
    public BaseResponse<List<QualityCheckRes>> batchExecute(
            @RequestParam(required = false) String datasourceId,
            @RequestParam(required = false) String tableName) {
        return BaseResponse.success(qualityService.batchExecute(datasourceId, tableName));
    }

    /**
     * Generate audit report for a datasource/table.
     */
    @GetMapping("/report")
    public BaseResponse<AuditReportRes> report(
            @RequestParam(required = false) String datasourceId,
            @RequestParam(required = false) String tableName) {
        return BaseResponse.success(qualityService.auditReport(datasourceId, tableName));
    }
}
