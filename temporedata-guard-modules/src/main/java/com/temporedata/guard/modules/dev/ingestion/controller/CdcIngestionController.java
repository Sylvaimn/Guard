package com.temporedata.guard.modules.dev.ingestion.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.common.eventbus.EventBus;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * CDC 实时采集演示 API: 发布模拟 binlog 变更事件 -> Kafka -> 消费者入湖。
 * 生产环境由 binlog 解析组件(Canal/Flink CDC)发布同格式事件。
 */
@RestController
@RequestMapping("/api/ingestion/cdc")
@RequiredArgsConstructor
public class CdcIngestionController {

    private final EventBus eventBus;
    private final com.temporedata.guard.modules.dev.ingestion.service.IngestionOffsetService offsetService;
    private final com.temporedata.guard.modules.common.lakehouse.service.LakehouseService lakehouseService;

    @PostMapping("/demo")
    public BaseResponse<String> demo(@AuthenticationPrincipal LoginUser user) {
        eventBus.publish("tdg-cdc", Map.of(
                "db", "tdg",
                "table", "track_flow_daily",
                "op", "INSERT",
                "ts", System.currentTimeMillis(),
                "rows", List.of(
                        Map.of("region", "昌平区", "stat_date", "2026-08-21", "renci", 66, "guiji", 488),
                        Map.of("region", "昌平区", "stat_date", "2026-08-22", "renci", 71, "guiji", 512))));
        return BaseResponse.success("CDC 事件已发布(2 行 -> Kafka tdg-cdc -> 湖仓)");
    }

    @GetMapping("/status")
    public BaseResponse<String> status(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success("CDC 采集链路就绪: binlog -> Kafka(tdg-cdc) -> Iceberg 湖仓");
    }

    /** 采集位点(断点续传状态) */
    @GetMapping("/offsets")
    public BaseResponse<java.util.List<java.util.Map<String, Object>>> offsets(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(offsetService.offsets());
    }

    /** CDC 采集对账: 源库(MySQL)行数 vs 湖仓(Iceberg)行数, 校验采集完整性 */
    @GetMapping("/reconcile")
    public BaseResponse<java.util.Map<String, Object>> reconcile(@AuthenticationPrincipal LoginUser user) {
        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        try {
            int mysqlRows = offsetService.countSourceRows("track_flow_daily");
            int lakeRows = lakehouseService.read("track_flow_daily").size();
            result.put("table", "track_flow_daily");
            result.put("sourceRows", mysqlRows);
            result.put("lakeRows", lakeRows);
            result.put("diff", mysqlRows - lakeRows);
            result.put("status", mysqlRows == lakeRows ? "SYNCED" : "MISMATCH");
        } catch (Exception e) {
            result.put("status", "ERROR");
            result.put("error", e.getMessage());
        }
        return BaseResponse.success(result);
    }
}
