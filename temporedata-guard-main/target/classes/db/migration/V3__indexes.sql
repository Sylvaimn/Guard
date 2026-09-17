-- V3: 高频查询索引(与实体 @Table(indexes) 保持一致; 表名/列名与 JPA 实体对齐)

-- 实体主数据
CREATE INDEX IF NOT EXISTS idx_em_tenant_type ON temporedata_guard_entity_master (tenant_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_em_tenant_status ON temporedata_guard_entity_master (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_em_pk_type ON temporedata_guard_entity_master (primary_key, entity_type);
CREATE INDEX IF NOT EXISTS idx_em_density ON temporedata_guard_entity_master (density);

-- 实体标签/映射
CREATE INDEX IF NOT EXISTS idx_el_entity ON temporedata_guard_entity_label (entity_id);
CREATE INDEX IF NOT EXISTS idx_el_code_tenant ON temporedata_guard_entity_label (label_code, tenant_id);
CREATE INDEX IF NOT EXISTS idx_el_type_codes_tenant ON temporedata_guard_entity_label (label_type, label_code, tenant_id);
CREATE INDEX IF NOT EXISTS idx_emap_entity ON temporedata_guard_entity_mapping (entity_id);
CREATE INDEX IF NOT EXISTS idx_emap_record ON temporedata_guard_entity_mapping (source_record_id);

-- 布控/预警
CREATE INDEX IF NOT EXISTS idx_wl_entity ON temporedata_guard_watch_list (entity_id);
CREATE INDEX IF NOT EXISTS idx_wl_tenant_status ON temporedata_guard_watch_list (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_wa_tenant_status ON temporedata_guard_watch_alert (tenant_id, status);
CREATE INDEX IF NOT EXISTS idx_wa_list ON temporedata_guard_watch_alert (watch_list_id);
CREATE INDEX IF NOT EXISTS idx_wa_entity ON temporedata_guard_watch_alert (entity_id);

-- 同步/采集任务
CREATE INDEX IF NOT EXISTS idx_st_tenant ON temporedata_guard_sync_task (tenant_id);
CREATE INDEX IF NOT EXISTS idx_st_status ON temporedata_guard_sync_task (status);
CREATE INDEX IF NOT EXISTS idx_it_tenant ON temporedata_guard_ingestion_task (tenant_id);
CREATE INDEX IF NOT EXISTS idx_it_status ON temporedata_guard_ingestion_task (status);

-- 黑名单
CREATE INDEX IF NOT EXISTS idx_bl_tenant_type_status ON temporedata_guard_blacklist (tenant_id, list_type, status);

-- 审计/API 日志
CREATE INDEX IF NOT EXISTS idx_al_tenant_time ON temporedata_guard_audit_log (tenant_id, create_time);
CREATE INDEX IF NOT EXISTS idx_apilog_tenant_time ON temporedata_guard_api_log (tenant_id, create_time);
