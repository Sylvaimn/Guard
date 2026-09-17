package com.temporedata.guard.modules.dev.catalog.service;
import com.temporedata.guard.modules.dev.meta.repository.MetaTableRepository;
import com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * One-time migration of legacy meta storage (temporedata_guard_meta_table / temporedata_guard_meta_column)
 * into the unified catalog storage (temporedata_guard_table_metadata / temporedata_guard_column_metadata).
 * Runs at startup only when the catalog is empty.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogMigrationRunner {

    private final CatalogService catalogService;
    private final com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository tableMetadataRepository;
    private final com.temporedata.guard.modules.dev.meta.repository.MetaTableRepository metaTableRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void migrateOnStartup() {
        if (tableMetadataRepository.count() > 0) {
            return;
        }
        if (metaTableRepository.count() == 0) {
            return;
        }
        int migrated = catalogService.migrateLegacyMeta();
        log.info("Catalog migration: {} tables migrated from legacy meta storage", migrated);
    }
}