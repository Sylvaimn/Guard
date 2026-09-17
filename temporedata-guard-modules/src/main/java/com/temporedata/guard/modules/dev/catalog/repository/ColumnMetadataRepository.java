package com.temporedata.guard.modules.dev.catalog.repository;

import com.temporedata.guard.modules.dev.catalog.entity.ColumnMetadataEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadataEntity, String> {

    List<ColumnMetadataEntity> findByTableMetadataIdOrderByOrdinalPosition(String tableMetadataId);

    List<ColumnMetadataEntity> findByTableMetadataId(String tableMetadataId);

    void deleteByTableMetadataId(String tableMetadataId);
}