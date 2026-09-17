package com.temporedata.guard.modules.dev.ingestion.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.ingestion.IngestionTaskReq;
import com.temporedata.guard.api.ingestion.IngestionTaskRes;
import com.temporedata.guard.modules.dev.ingestion.service.IngestionService;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.security.security.audit.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ingestion")
@RequiredArgsConstructor
public class IngestionController {

    private final IngestionService ingestionService;

    @GetMapping
    public BaseResponse<List<IngestionTaskRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(ingestionService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<IngestionTaskRes> detail(@PathVariable String id) {
        return BaseResponse.success(ingestionService.detail(id));
    }

    @PostMapping
    @Audit(action = "CREATE", targetType = "INGESTION", targetName = "#{#req.name}")
    public BaseResponse<IngestionTaskRes> create(@RequestBody IngestionTaskReq req,
                                                  @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(ingestionService.create(req, user.getTenantId()));
    }

    @Audit(action = "DELETE", targetType = "INGESTION", targetName = "#{#id}")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        ingestionService.delete(id);
        return BaseResponse.success();
    }

    @Audit(action = "EXECUTE", targetType = "INGESTION", targetName = "#{#id}", detail = "上传CSV执行采集")
    @PostMapping("/{id}/execute")
    public BaseResponse<Void> execute(@PathVariable String id, @RequestParam("file") MultipartFile file)
            throws java.io.IOException {
        ingestionService.executeAsync(id, file.getBytes());
        return BaseResponse.success();
    }
}