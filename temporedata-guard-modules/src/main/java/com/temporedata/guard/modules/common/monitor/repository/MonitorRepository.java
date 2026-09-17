package com.temporedata.guard.modules.common.monitor.repository;

import com.temporedata.guard.modules.common.monitor.entity.MonitorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MonitorRepository extends JpaRepository<MonitorEntity, String> {

    List<MonitorEntity> findByClusterId(String clusterId);

    List<MonitorEntity> findByClusterNodeId(String clusterNodeId);

    @Query("SELECT m FROM MonitorEntity m WHERE m.clusterId = :clusterId AND m.createDateTime BETWEEN :start AND :end ORDER BY m.createDateTime ASC")
    List<MonitorEntity> findByClusterIdAndTimeRange(
            @Param("clusterId") String clusterId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    void deleteByCreateDateTimeBefore(LocalDateTime before);
}