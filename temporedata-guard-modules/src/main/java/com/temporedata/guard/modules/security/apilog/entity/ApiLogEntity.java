/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.persistence.Column
 *  javax.persistence.Entity
 *  javax.persistence.Id
 *  javax.persistence.Index
 *  javax.persistence.Table
 */
package com.temporedata.guard.modules.security.apilog.entity;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;

@Entity
@Table(name="temporedata_guard_api_log", indexes={@Index(name="idx_apilog_tenant_time", columnList="tenantId, createTime")})
public class ApiLogEntity {
    @Id
    @Column(length=32)
    private String id;
    @Column(length=128)
    private String apiName;
    @Column(length=256)
    private String apiPath;
    @Column(length=16)
    private String method;
    @Column(length=64)
    private String requestIp;
    @Column(length=4096)
    private String requestParams;
    private Integer responseCode;
    @Column(length=8192)
    private String responseBody;
    private Long costTime;
    @Column(length=32)
    private String callerId;
    @Column(nullable=false, length=32)
    private String tenantId;
    @Column(nullable=false)
    private LocalDateTime createTime;

    public String getId() {
        return this.id;
    }

    public String getApiName() {
        return this.apiName;
    }

    public String getApiPath() {
        return this.apiPath;
    }

    public String getMethod() {
        return this.method;
    }

    public String getRequestIp() {
        return this.requestIp;
    }

    public String getRequestParams() {
        return this.requestParams;
    }

    public Integer getResponseCode() {
        return this.responseCode;
    }

    public String getResponseBody() {
        return this.responseBody;
    }

    public Long getCostTime() {
        return this.costTime;
    }

    public String getCallerId() {
        return this.callerId;
    }

    public String getTenantId() {
        return this.tenantId;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public void setApiPath(String apiPath) {
        this.apiPath = apiPath;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    public void setCostTime(Long costTime) {
        this.costTime = costTime;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof ApiLogEntity)) {
            return false;
        }
        ApiLogEntity other = (ApiLogEntity)o;
        if (!other.canEqual(this)) {
            return false;
        }
        Integer this$responseCode = this.getResponseCode();
        Integer other$responseCode = other.getResponseCode();
        if (this$responseCode == null ? other$responseCode != null : !((Object)this$responseCode).equals(other$responseCode)) {
            return false;
        }
        Long this$costTime = this.getCostTime();
        Long other$costTime = other.getCostTime();
        if (this$costTime == null ? other$costTime != null : !((Object)this$costTime).equals(other$costTime)) {
            return false;
        }
        String this$id = this.getId();
        String other$id = other.getId();
        if (this$id == null ? other$id != null : !this$id.equals(other$id)) {
            return false;
        }
        String this$apiName = this.getApiName();
        String other$apiName = other.getApiName();
        if (this$apiName == null ? other$apiName != null : !this$apiName.equals(other$apiName)) {
            return false;
        }
        String this$apiPath = this.getApiPath();
        String other$apiPath = other.getApiPath();
        if (this$apiPath == null ? other$apiPath != null : !this$apiPath.equals(other$apiPath)) {
            return false;
        }
        String this$method = this.getMethod();
        String other$method = other.getMethod();
        if (this$method == null ? other$method != null : !this$method.equals(other$method)) {
            return false;
        }
        String this$requestIp = this.getRequestIp();
        String other$requestIp = other.getRequestIp();
        if (this$requestIp == null ? other$requestIp != null : !this$requestIp.equals(other$requestIp)) {
            return false;
        }
        String this$requestParams = this.getRequestParams();
        String other$requestParams = other.getRequestParams();
        if (this$requestParams == null ? other$requestParams != null : !this$requestParams.equals(other$requestParams)) {
            return false;
        }
        String this$responseBody = this.getResponseBody();
        String other$responseBody = other.getResponseBody();
        if (this$responseBody == null ? other$responseBody != null : !this$responseBody.equals(other$responseBody)) {
            return false;
        }
        String this$callerId = this.getCallerId();
        String other$callerId = other.getCallerId();
        if (this$callerId == null ? other$callerId != null : !this$callerId.equals(other$callerId)) {
            return false;
        }
        String this$tenantId = this.getTenantId();
        String other$tenantId = other.getTenantId();
        if (this$tenantId == null ? other$tenantId != null : !this$tenantId.equals(other$tenantId)) {
            return false;
        }
        LocalDateTime this$createTime = this.getCreateTime();
        LocalDateTime other$createTime = other.getCreateTime();
        return !(this$createTime == null ? other$createTime != null : !((Object)this$createTime).equals(other$createTime));
    }

    protected boolean canEqual(Object other) {
        return other instanceof ApiLogEntity;
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Integer $responseCode = this.getResponseCode();
        result = result * 59 + ($responseCode == null ? 43 : ((Object)$responseCode).hashCode());
        Long $costTime = this.getCostTime();
        result = result * 59 + ($costTime == null ? 43 : ((Object)$costTime).hashCode());
        String $id = this.getId();
        result = result * 59 + ($id == null ? 43 : $id.hashCode());
        String $apiName = this.getApiName();
        result = result * 59 + ($apiName == null ? 43 : $apiName.hashCode());
        String $apiPath = this.getApiPath();
        result = result * 59 + ($apiPath == null ? 43 : $apiPath.hashCode());
        String $method = this.getMethod();
        result = result * 59 + ($method == null ? 43 : $method.hashCode());
        String $requestIp = this.getRequestIp();
        result = result * 59 + ($requestIp == null ? 43 : $requestIp.hashCode());
        String $requestParams = this.getRequestParams();
        result = result * 59 + ($requestParams == null ? 43 : $requestParams.hashCode());
        String $responseBody = this.getResponseBody();
        result = result * 59 + ($responseBody == null ? 43 : $responseBody.hashCode());
        String $callerId = this.getCallerId();
        result = result * 59 + ($callerId == null ? 43 : $callerId.hashCode());
        String $tenantId = this.getTenantId();
        result = result * 59 + ($tenantId == null ? 43 : $tenantId.hashCode());
        LocalDateTime $createTime = this.getCreateTime();
        result = result * 59 + ($createTime == null ? 43 : ((Object)$createTime).hashCode());
        return result;
    }

    public String toString() {
        return "ApiLogEntity(id=" + this.getId() + ", apiName=" + this.getApiName() + ", apiPath=" + this.getApiPath() + ", method=" + this.getMethod() + ", requestIp=" + this.getRequestIp() + ", requestParams=" + this.getRequestParams() + ", responseCode=" + this.getResponseCode() + ", responseBody=" + this.getResponseBody() + ", costTime=" + this.getCostTime() + ", callerId=" + this.getCallerId() + ", tenantId=" + this.getTenantId() + ", createTime=" + this.getCreateTime() + ")";
    }
}

