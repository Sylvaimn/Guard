package com.temporedata.guard.modules.dev.tag.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Binding between a tag and a data asset (table / indicator / datasource).
 */
@Data
@Entity
@Table(name = "temporedata_guard_tag_binding")
public class TagBindingEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(name = "tag_id", length = 32)
    private String tagId;

    @Column(name = "asset_type", length = 32)
    private String assetType;

    @Column(name = "asset_id", length = 64)
    private String assetId;

    @Column(name = "asset_name", length = 128)
    private String assetName;

    @Column(name = "tenant_id", length = 32)
    private String tenantId;

    @Column(name = "create_time")
    private LocalDateTime createTime;
}