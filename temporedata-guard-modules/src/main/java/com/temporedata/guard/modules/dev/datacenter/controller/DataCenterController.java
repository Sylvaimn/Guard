package com.temporedata.guard.modules.dev.datacenter.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.datacenter.DataCenterOverviewRes;
import com.temporedata.guard.api.datacenter.DataCenterReq;
import com.temporedata.guard.api.datacenter.DataCenterRes;
import com.temporedata.guard.modules.dev.datacenter.entity.DataCenterEntity;
import com.temporedata.guard.modules.dev.datacenter.service.DataCenterService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Data center REST API.
 */
@RestController
@RequestMapping("/api/datacenter")
@RequiredArgsConstructor
public class DataCenterController {

    private final DataCenterService dataCenterService;

    @GetMapping("/list")
    public BaseResponse<List<DataCenterRes>> list(@RequestParam(required = false) String category) {
        if (category != null) {
            return BaseResponse.success(dataCenterService.listByCategory(category));
        }
        return BaseResponse.success(dataCenterService.listAll());
    }

    @PostMapping("/create")
    public BaseResponse<DataCenterRes> create(@RequestBody DataCenterReq req,
                                              @AuthenticationPrincipal LoginUser user) {
        DataCenterEntity entity = new DataCenterEntity();
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setCategory(req.getCategory());
        entity.setConfig(req.getConfig());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setRefreshInterval(req.getRefreshInterval());
        entity.setTenantId(user.getTenantId());
        return BaseResponse.success(dataCenterService.create(entity));
    }

    @PutMapping("/{id}")
    public BaseResponse<DataCenterRes> update(@PathVariable String id, @RequestBody DataCenterReq req) {
        DataCenterEntity entity = new DataCenterEntity();
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setCategory(req.getCategory());
        entity.setConfig(req.getConfig());
        entity.setDatasourceId(req.getDatasourceId());
        entity.setRefreshInterval(req.getRefreshInterval());
        return BaseResponse.success(dataCenterService.update(id, entity));
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        dataCenterService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping("/overview")
    public BaseResponse<DataCenterOverviewRes> overview() {
        return BaseResponse.success(dataCenterService.getOverview());
    }
}