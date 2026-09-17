package com.temporedata.guard.modules.dev.meta.repository;

import com.temporedata.guard.modules.dev.meta.entity.MetaColumnEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MetaColumnRepository extends JpaRepository<MetaColumnEntity, String> {

    List<MetaColumnEntity> findByTableId(String tableId);

    List<MetaColumnEntity> findByTableIdOrderByOrdinalPositionAsc(String tableId);

    void deleteByTableId(String tableId);

    long countByTableId(String tableId);
}