package com.temporedata.guard.modules.police.track.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.police.track.entity.TrackPointEntity;
import com.temporedata.guard.modules.police.track.repository.TrackPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 轨迹分析服务（P1）：轨迹点录入 / 时段查询 / 落脚点聚合 / 活动规律。
 * 可注册为 Agent 工具(track_query)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackService {

    private final TrackPointRepository trackPointRepository;

    /** 录入轨迹点(批量) */
    @Transactional
    public int addPoints(String entityId, String entityType, List<Map<String, Object>> points, String tenantId) {
        if (entityId == null || entityId.isBlank()) throw new BusinessException("轨迹对象必填");
        int count = 0;
        for (Map<String, Object> p : points) {
            TrackPointEntity t = new TrackPointEntity();
            t.setId(UUID.randomUUID().toString().replace("-", ""));
            t.setEntityId(entityId);
            t.setEntityType(entityType == null ? "PERSON" : entityType);
            t.setLat(p.get("lat") == null ? null : Double.valueOf(String.valueOf(p.get("lat"))));
            t.setLng(p.get("lng") == null ? null : Double.valueOf(String.valueOf(p.get("lng"))));
            t.setScene(p.get("scene") == null ? "卡口" : String.valueOf(p.get("scene")));
            t.setAddress(p.get("address") == null ? null : String.valueOf(p.get("address")));
            t.setTrackTime(p.get("time") == null ? LocalDateTime.now()
                    : LocalDateTime.parse(String.valueOf(p.get("time"))));
            t.setTenantId(tenantId);
            t.setCreateTime(LocalDateTime.now());
            trackPointRepository.save(t);
            count++;
        }
        return count;
    }

    /** 时段轨迹查询 */
    public List<TrackPointEntity> query(String entityId, LocalDateTime from, LocalDateTime to, String tenantId) {
        List<TrackPointEntity> points = from != null && to != null
                ? trackPointRepository.findByEntityIdAndTrackTimeBetweenOrderByTrackTimeAsc(entityId, from, to)
                : trackPointRepository.findByEntityIdOrderByTrackTimeAsc(entityId);
        return points.stream().filter(p -> p.getTenantId().equals(tenantId)).collect(Collectors.toList());
    }

    /** 落脚点聚合: 按地址/场景统计频次与首次/末次时间 */
    public List<Map<String, Object>> footprints(String entityId, LocalDateTime from, LocalDateTime to, String tenantId) {
        List<TrackPointEntity> points = query(entityId, from, to, tenantId);
        Map<String, List<TrackPointEntity>> byPlace = points.stream()
                .collect(Collectors.groupingBy(p -> p.getAddress() != null && !p.getAddress().isBlank()
                        ? p.getAddress() : (p.getScene() == null ? "未知" : p.getScene())));
        return byPlace.entrySet().stream()
                .map(e -> {
                    List<TrackPointEntity> list = e.getValue();
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("place", e.getKey());
                    m.put("count", list.size());
                    m.put("firstTime", list.get(0).getTrackTime() == null ? "" : list.get(0).getTrackTime().toString());
                    m.put("lastTime", list.get(list.size() - 1).getTrackTime() == null ? "" : list.get(list.size() - 1).getTrackTime().toString());
                    m.put("scenes", list.stream().map(TrackPointEntity::getScene).distinct().collect(Collectors.toList()));
                    return m;
                })
                .sorted((a, b) -> Integer.compare((Integer) b.get("count"), (Integer) a.get("count")))
                .collect(Collectors.toList());
    }

    /** 活动规律摘要: 活跃时段/主要场景(供 Agent 文本化) */
    public Map<String, Object> summary(String entityId, LocalDateTime from, LocalDateTime to, String tenantId) {
        List<TrackPointEntity> points = query(entityId, from, to, tenantId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pointCount", points.size());
        out.put("firstTime", points.isEmpty() || points.get(0).getTrackTime() == null ? "" : points.get(0).getTrackTime().toString());
        out.put("lastTime", points.isEmpty() || points.get(points.size() - 1).getTrackTime() == null ? "" : points.get(points.size() - 1).getTrackTime().toString());
        // 主要场景
        out.put("topScenes", points.stream().collect(Collectors.groupingBy(TrackPointEntity::getScene, Collectors.counting()))
                .entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(5).map(e -> e.getKey() + "×" + e.getValue()).collect(Collectors.toList()));
        // 活跃时段(按小时)
        Map<Integer, Long> byHour = points.stream().collect(Collectors.groupingBy(
                p -> p.getTrackTime() == null ? 0 : p.getTrackTime().getHour(), Collectors.counting()));
        out.put("activeHours", byHour.entrySet().stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(3).map(e -> e.getKey() + "时×" + e.getValue()).collect(Collectors.toList()));
        out.put("footprints", footprints(entityId, from, to, tenantId));
        return out;
    }
}
