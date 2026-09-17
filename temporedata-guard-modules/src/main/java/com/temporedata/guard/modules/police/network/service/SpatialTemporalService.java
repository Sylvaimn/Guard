package com.temporedata.guard.modules.police.network.service;

import com.temporedata.guard.modules.police.track.entity.TrackPointEntity;
import com.temporedata.guard.modules.police.track.repository.TrackPointRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 时空分析（P2）：轨迹/警情点时空聚合 → 热点/时段分布/区域态势。
 * 复用 track_point 数据(点位带场景/地址/时间)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpatialTemporalService {

    private final TrackPointRepository trackPointRepository;

    /** 热点区域 Top: 按地址聚合点密度 */
    public List<Map<String, Object>> hotSpots(String tenantId, LocalDateTime from, LocalDateTime to, int topN) {
        List<TrackPointEntity> points = pointsInRange(tenantId, from, to);
        return points.stream()
                .collect(Collectors.groupingBy(p -> p.getAddress() != null && !p.getAddress().isBlank()
                        ? p.getAddress() : (p.getScene() == null ? "未知" : p.getScene()), Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN == 0 ? 10 : topN)
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("area", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 时段分布: 24 小时点密度 */
    public List<Map<String, Object>> hourDistribution(String tenantId, LocalDateTime from, LocalDateTime to) {
        List<TrackPointEntity> points = pointsInRange(tenantId, from, to);
        Map<Integer, Long> byHour = points.stream()
                .collect(Collectors.groupingBy(p -> p.getTrackTime() == null ? 0 : p.getTrackTime().getHour(),
                        Collectors.counting()));
        return byHour.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("hour", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 场景态势: 各场景点密度(卡口/酒店/小区…) */
    public List<Map<String, Object>> sceneTrend(String tenantId, LocalDateTime from, LocalDateTime to) {
        List<TrackPointEntity> points = pointsInRange(tenantId, from, to);
        return points.stream()
                .collect(Collectors.groupingBy(p -> p.getScene() == null ? "未知" : p.getScene(), Collectors.counting()))
                .entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("scene", e.getKey());
                    m.put("count", e.getValue());
                    return m;
                })
                .collect(Collectors.toList());
    }

    /** 总体态势卡 */
    public Map<String, Object> overview(String tenantId, LocalDateTime from, LocalDateTime to) {
        List<TrackPointEntity> points = pointsInRange(tenantId, from, to);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("totalPoints", points.size());
        out.put("activeEntities", points.stream().map(TrackPointEntity::getEntityId).distinct().count());
        out.put("hotSpots", hotSpots(tenantId, from, to, 5));
        out.put("hourDistribution", hourDistribution(tenantId, from, to));
        out.put("sceneTrend", sceneTrend(tenantId, from, to));
        return out;
    }

    private List<TrackPointEntity> pointsInRange(String tenantId, LocalDateTime from, LocalDateTime to) {
        LocalDateTime f = from == null ? LocalDateTime.now().minusDays(30) : from;
        LocalDateTime t = to == null ? LocalDateTime.now() : to;
        return trackPointRepository.findByTrackTimeBetweenOrderByTrackTimeAsc(f, t).stream()
                .filter(p -> p.getTenantId().equals(tenantId))
                .collect(Collectors.toList());
    }
}
