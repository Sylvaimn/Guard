package com.temporedata.guard.modules.common.lakehouse.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.common.lakehouse.service.LakehouseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据湖仓 API: 建表/写入/读取/快照(时间旅行)。
 */
@RestController
@RequestMapping("/api/lakehouse")
@RequiredArgsConstructor
public class LakehouseController {

    private final LakehouseService lakehouseService;

    @GetMapping("/init")
    public BaseResponse<Map<String, Object>> init() {
        lakehouseService.createTable(LakehouseService.TABLE);
        return BaseResponse.success(Map.of("warehouse", lakehouseService.getWarehouse(), "table", "temporedata_guard.temporedata_guard_track_flow_daily"));
    }

    @PostMapping("/append")
    public BaseResponse<Integer> append(@RequestBody List<Map<String, Object>> rows) {
        try { return BaseResponse.success(lakehouseService.append(LakehouseService.TABLE, rows)); }
        catch (java.io.IOException e) { throw new com.temporedata.guard.api.base.exceptions.BusinessException("湖仓写入失败: " + e.getMessage()); }
    }

    @GetMapping("/read")
    public BaseResponse<List<Map<String, Object>>> read(@RequestParam(required = false) Long snapshotId) {
        try { return BaseResponse.success(lakehouseService.read(LakehouseService.TABLE, snapshotId)); }
        catch (java.io.IOException e) { throw new com.temporedata.guard.api.base.exceptions.BusinessException("湖仓读取失败: " + e.getMessage()); }
    }

    @GetMapping("/snapshots")
    public BaseResponse<List<Map<String, Object>>> snapshots() {
        return BaseResponse.success(lakehouseService.snapshots(LakehouseService.TABLE));
    }
}
