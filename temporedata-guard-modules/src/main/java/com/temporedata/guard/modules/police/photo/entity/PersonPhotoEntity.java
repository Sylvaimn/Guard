package com.temporedata.guard.modules.police.photo.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * 人员照片库（P1）：实体人员照片(证件照/现场照/前科照)，特征加密存储。
 */
@Data
@Entity
@Table(name = "temporedata_guard_person_photo", indexes = {
        @javax.persistence.Index(name = "idx_pp_entity", columnList = "entityId"),
        @javax.persistence.Index(name = "idx_pp_tenant", columnList = "tenantId")
})
public class PersonPhotoEntity {

    @Id
    @Column(length = 32)
    private String id;

    /** 关联人员实体 */
    @Column(nullable = false, length = 32)
    private String entityId;

    /** 照片文件(file 模块 fileId) */
    @Column(nullable = false, length = 32)
    private String fileId;

    /** ID(证件照) / SCENE(现场照) / MUG(前科照) */
    @Column(nullable = false, length = 16)
    private String photoType = "SCENE";

    /** 特征(AES 加密, 生物信息保护) */
    @Column(length = 4000)
    private String feature;

    /** 照片密级(默认 CONFIDENTIAL) */
    @Column(nullable = false, length = 32)
    private String density = "CONFIDENTIAL";

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
