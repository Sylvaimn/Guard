package com.temporedata.guard.modules.common.ha.repository;

import com.temporedata.guard.modules.common.ha.entity.HaServerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HaServerRepository extends JpaRepository<HaServerEntity, String> {

    List<HaServerEntity> findByStatus(String status);

    List<HaServerEntity> findByLastHeartbeatBefore(LocalDateTime threshold);

    HaServerEntity findByHostAndPort(String host, Integer port);
}