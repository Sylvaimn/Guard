package com.temporedata.guard.modules.dev.tag.entity;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

/**
 * Data tag definition.
 */
@Data
@Entity
@Table(name = "temporedata_guard_tag")
public class TagEntity {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(length = 32, unique = true)
    private String code;

    @Column(length = 255)
    private String description;

    @Column(length = 16)
    private String color; // hex color code

    @Column(name = "classification_id", length = 32)
    private String classificationId;

    @Column(name = "is_mutually_exclusive")
    private Boolean isMutuallyExclusive; // tags in same classification are mutually exclusive

    @Column(length = 16)
    private String status; // DRAFT, APPROVED

    @Column(name = "derived_from_tag_id", length = 32)
    private String derivedFromTagId; // auto-derived from parent tag when parent is bound

    @Column(length = 2000)
    private String definition; // authoritative definition for term tags

    @Column(length = 255)
    private String synonyms; // comma separated synonyms for term tags

    @Column(name = "is_term")
    private Boolean isTerm; // true = glossary term (with definition/synonyms)

    @Column(nullable = false, length = 32)
    private String tenantId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}