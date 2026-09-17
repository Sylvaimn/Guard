/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.alarm.AlarmConfigReq
 *  com.temporedata.guard.api.alarm.AlarmConfigRes
 *  com.temporedata.guard.api.alarm.AlarmRecordRes
 *  com.temporedata.guard.api.base.pojos.BaseResponse
 *  com.temporedata.guard.security.LoginUser
 *  org.springframework.security.core.annotation.AuthenticationPrincipal
 *  org.springframework.web.bind.annotation.DeleteMapping
 *  org.springframework.web.bind.annotation.GetMapping
 *  org.springframework.web.bind.annotation.PathVariable
 *  org.springframework.web.bind.annotation.PostMapping
 *  org.springframework.web.bind.annotation.PutMapping
 *  org.springframework.web.bind.annotation.RequestBody
 *  org.springframework.web.bind.annotation.RequestMapping
 *  org.springframework.web.bind.annotation.RestController
 */
package com.temporedata.guard.modules.security.alarm.controller;
import com.temporedata.guard.modules.security.alarm.service.AlarmService;

import com.temporedata.guard.api.alarm.AlarmConfigReq;
import com.temporedata.guard.api.alarm.AlarmConfigRes;
import com.temporedata.guard.api.alarm.AlarmRecordRes;
import com.temporedata.guard.api.base.pojos.BaseResponse;

import com.temporedata.guard.modules.security.security.audit.Audit;
import com.temporedata.guard.security.LoginUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/api/alarm/config"})
public class AlarmController {
    private final AlarmService alarmService;

    @GetMapping
    public BaseResponse<List<AlarmConfigRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.alarmService.list(user.getTenantId()));
    }

    @GetMapping(value={"/{id}"})
    public BaseResponse<AlarmConfigRes> detail(@PathVariable String id) {
        return BaseResponse.success(this.alarmService.detail(id));
    }

    @PostMapping
    @Audit(action="CREATE", targetType="ALARM_CONFIG", targetName="#{#req.name}")
    public BaseResponse<AlarmConfigRes> create(@RequestBody AlarmConfigReq req, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(this.alarmService.create(req, user.getTenantId()));
    }

    @Audit(action="UPDATE", targetType="ALARM_CONFIG", targetName="#{#id}")
    @PutMapping(value={"/{id}"})
    public BaseResponse<AlarmConfigRes> update(@PathVariable String id, @RequestBody AlarmConfigReq req) {
        return BaseResponse.success(this.alarmService.update(id, req));
    }

    @Audit(action="DELETE", targetType="ALARM_CONFIG", targetName="#{#id}")
    @DeleteMapping(value={"/{id}"})
    public BaseResponse<Void> delete(@PathVariable String id) {
        this.alarmService.delete(id);
        return BaseResponse.success();
    }

    @GetMapping(value={"/{id}/records"})
    public BaseResponse<List<AlarmRecordRes>> listRecords(@PathVariable String id) {
        return BaseResponse.success(this.alarmService.listRecords(id));
    }

    @Audit(action="EXECUTE", targetType="ALARM_CONFIG", targetName="#{#id}", detail="\u6d4b\u8bd5\u544a\u8b66")
    @PostMapping(value={"/{id}/test"})
    public BaseResponse<String> testAlarm(@PathVariable String id) {
        return BaseResponse.success(this.alarmService.testAlarm(id));
    }

    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }
}

