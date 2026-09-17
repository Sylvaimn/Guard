package com.temporedata.guard.modules.data.datasource.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.datasource.DatasourceReq;
import com.temporedata.guard.api.datasource.DatasourceRes;
import com.temporedata.guard.modules.data.datasource.service.DatasourceService;
import com.temporedata.guard.security.LoginUser;
import com.temporedata.guard.modules.security.security.audit.Audit;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/datasource")
@RequiredArgsConstructor
public class DatasourceController {

    private final DatasourceService datasourceService;

    @GetMapping
    public BaseResponse<List<DatasourceRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(datasourceService.list(user.getTenantId()));
    }

    @GetMapping("/{id}")
    public BaseResponse<DatasourceRes> detail(@PathVariable String id) {
        return BaseResponse.success(datasourceService.detail(id));
    }

    @PostMapping
    @Audit(action = "CREATE", targetType = "DATASOURCE", targetName = "#{#req.name}")
    public BaseResponse<DatasourceRes> create(@RequestBody DatasourceReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(datasourceService.create(req, user.getTenantId()));
    }

    @Audit(action = "UPDATE", targetType = "DATASOURCE", targetName = "#{#id}")
    @PutMapping("/{id}")
    public BaseResponse<DatasourceRes> update(@PathVariable String id, @RequestBody DatasourceReq req) {
        return BaseResponse.success(datasourceService.update(id, req));
    }

    @Audit(action = "DELETE", targetType = "DATASOURCE", targetName = "#{#id}")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        datasourceService.delete(id);
        return BaseResponse.success();
    }

    @PostMapping("/{id}/test")
    public BaseResponse<Boolean> testConnection(@PathVariable String id) {
        return BaseResponse.success(datasourceService.testConnection(id));
    }

    /** 已注册插件类型与能力(前端按能力渲染) */
    @GetMapping("/plugins")
    public BaseResponse<java.util.Map<String, String[]>> pluginTypes() {
        return BaseResponse.success(datasourceService.pluginTypes());
    }

    /** 插件元数据采集(湖仓快照/Kafka topic/ClickHouse 表/JDBC 表清单) */
    @GetMapping("/{id}/meta")
    public BaseResponse<java.util.List<java.util.Map<String, Object>>> pluginMeta(@PathVariable String id) {
        return BaseResponse.success(datasourceService.pluginMeta(id));
    }

    /**
     * Execute SQL on a datasource (used to initialize sample tables for testing).
     */
    @PostMapping("/{id}/execute")
    public BaseResponse<Integer> executeSql(@PathVariable String id, @RequestBody java.util.Map<String, Object> body) {
        String sql = (String) body.get("sql");
        return BaseResponse.success(datasourceService.executeSql(id, sql));
    }

    /**
     * Query a datasource and return rows.
     */
    @PostMapping("/{id}/query")
    public BaseResponse<java.util.List<java.util.Map<String, Object>>> querySql(
            @PathVariable String id, @RequestBody java.util.Map<String, Object> body) {
        String sql = (String) body.get("sql");
        return BaseResponse.success(datasourceService.querySql(id, sql));
    }
}