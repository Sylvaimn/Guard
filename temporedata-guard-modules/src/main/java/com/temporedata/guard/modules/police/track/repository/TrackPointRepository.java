package com.temporedata.guard.modules.police.track.repository;

import com.temporedata.guard.modules.police.track.entity.TrackPointEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TrackPointRepository extends JpaRepository<TrackPointEntity, String> {

    List<TrackPointEntity> findByEntityIdAndTrackTimeBetweenOrderByTrackTimeAsc(
            String entityId, LocalDateTime from, LocalDateTime to);

    List<TrackPointEntity> findByEntityIdOrderByTrackTimeAsc(String entityId);

    List<TrackPointEntity> findByTrackTimeBetweenOrderByTrackTimeAsc(java.time.LocalDateTime from, java.time.LocalDateTime to);

    long countByEntityIdAndTrackTimeBetween(String entityId, LocalDateTime from, LocalDateTime to);
}
