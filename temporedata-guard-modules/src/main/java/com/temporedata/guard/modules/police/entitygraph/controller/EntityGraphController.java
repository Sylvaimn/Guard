package com.temporedata.guard.modules.police.entitygraph.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.entitygraph.EntityGraphRes;
import com.temporedata.guard.modules.police.entitygraph.service.EntityGraphService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/entitygraph")
@RequiredArgsConstructor
public class EntityGraphController {

    private final EntityGraphService entityGraphService;

    /** 实体关联子图（服务端裁剪）：一查多 / N度 / 边类型过滤 */
    @GetMapping("/{entityId}")
    public BaseResponse<EntityGraphRes> graph(@PathVariable String entityId,
                                              @RequestParam(required = false) Integer degree,
                                              @RequestParam(required = false) String edgeType,
                                              @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(entityGraphService.getGraph(entityId, degree, edgeType, user.getTenantId()));
    }
}
