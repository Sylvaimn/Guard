package com.temporedata.guard.modules.dev.datacenter.repository;

import com.temporedata.guard.modules.dev.datacenter.entity.DataCenterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Data center repository.
 */
public interface DataCenterRepository extends JpaRepository<DataCenterEntity, String> {

    List<DataCenterEntity> findByCategory(String category);
}