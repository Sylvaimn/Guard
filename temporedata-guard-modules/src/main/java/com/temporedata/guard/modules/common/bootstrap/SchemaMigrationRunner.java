package com.temporedata.guard.modules.common.bootstrap;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Idempotent schema migration for H2 dev database.
 * ddl-auto only creates missing tables but never alters existing ones,
 * so new columns on legacy tables must be added explicitly.
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaMigrationRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        List<String> statements = Arrays.asList(
                "ALTER TABLE temporedata_guard_role ADD COLUMN IF NOT EXISTS remark VARCHAR(255)",
                "ALTER TABLE temporedata_guard_role ADD COLUMN IF NOT EXISTS protected_role BOOLEAN DEFAULT FALSE",
                "ALTER TABLE temporedata_guard_tenant ADD COLUMN IF NOT EXISTS code VARCHAR(32)",
                "ALTER TABLE temporedata_guard_user ADD COLUMN IF NOT EXISTS density VARCHAR(32) DEFAULT 'INTERNAL'",
                "ALTER TABLE temporedata_guard_sync_task ADD COLUMN IF NOT EXISTS upsert_key VARCHAR(64)",
                "ALTER TABLE temporedata_guard_sync_task ADD COLUMN IF NOT EXISTS schedule_cron VARCHAR(64)",
                "ALTER TABLE temporedata_guard_tenant ADD COLUMN IF NOT EXISTS contact_name VARCHAR(64)",
                "ALTER TABLE temporedata_guard_tenant ADD COLUMN IF NOT EXISTS contact_email VARCHAR(128)",
                "ALTER TABLE temporedata_guard_org ADD COLUMN IF NOT EXISTS responsible_user_id VARCHAR(32)",
                "ALTER TABLE temporedata_guard_org ADD COLUMN IF NOT EXISTS default_role_id VARCHAR(32)"
        );
        for (String sql : statements) {
            try {
                jdbcTemplate.execute(sql);
            } catch (Exception e) {
                log.warn("Schema migration skipped: {} ({})", sql, e.getMessage());
            }
        }
        log.info("Schema migration completed");
    }
}
