package com.temporedata.guard.modules.police.photo.service;
import com.temporedata.guard.modules.security.security.audit.Audit;
import com.temporedata.guard.modules.police.photo.entity.CompareCandidateEntity;
import com.temporedata.guard.modules.police.photo.repository.CompareCandidateRepository;
import com.temporedata.guard.modules.police.photo.entity.CompareReportEntity;
import com.temporedata.guard.modules.police.photo.repository.CompareReportRepository;
import com.temporedata.guard.modules.police.photo.entity.CompareTaskEntity;
import com.temporedata.guard.modules.police.photo.repository.CompareTaskRepository;
import com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository;
import com.temporedata.guard.modules.police.photo.entity.PersonPhotoEntity;
import com.temporedata.guard.modules.police.photo.repository.PersonPhotoRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.intelligence.IntelligenceRes;
import com.temporedata.guard.modules.police.entitymaster.service.EntityMasterService;
import com.temporedata.guard.modules.police.intelligence.service.IntelligenceService;
import com.temporedata.guard.modules.police.watch.service.WatchService;
import com.temporedata.guard.security.LoginUser;
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
 * 以图找人业务服务（P1）：
 * 上传照片(+线索词) → 生成候选(降级: 线索词关键词检索; 预留特征化算法) → 人工复核闭环
 * → 布控命中标注 → 比对报告取证。全程 @Audit 留痕。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PhotoCompareService {

    private final PersonPhotoRepository personPhotoRepository;
    private final CompareTaskRepository taskRepository;
    private final CompareCandidateRepository candidateRepository;
    private final CompareReportRepository reportRepository;
    private final IntelligenceService intelligenceService;
    private final EntityMasterService entityMasterService;
    private final WatchService watchService;
    private final com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository labelRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int DEFAULT_TOP_K = 5;

    // ==================== 照片库 ====================

    @Transactional
    public PersonPhotoEntity addPersonPhoto(String entityId, String fileId, String photoType, LoginUser user) {
        PersonPhotoEntity p = new PersonPhotoEntity();
        p.setId(UUID.randomUUID().toString().replace("-", ""));
        p.setEntityId(entityId);
        p.setFileId(fileId);
        p.setPhotoType(photoType == null ? "SCENE" : photoType);
        p.setTenantId(user.getTenantId());
        p.setCreateTime(LocalDateTime.now());
        return personPhotoRepository.save(p);
    }

    public List<PersonPhotoEntity> listPersonPhotos(String entityId, LoginUser user) {
        return personPhotoRepository.findByEntityIdOrderByCreateTimeDesc(entityId).stream()
                .filter(p -> p.getTenantId().equals(user.getTenantId()))
                .collect(Collectors.toList());
    }

    // ==================== 比对任务 ====================

    /**
     * 创建比对任务: 照片 + 线索词 → 候选生成 → REVIEWING(等待人工复核)
     */
    @Transactional
    public CompareTaskEntity createTask(String fileId, String keywords, String caseId, String source,
                                         String database, LoginUser user) {
        if (fileId == null || fileId.isBlank()) {
            throw new BusinessException("比对照片(fileId)必填");
        }
        CompareTaskEntity task = new CompareTaskEntity();
        task.setId(UUID.randomUUID().toString().replace("-", ""));
        task.setFileId(fileId);
        task.setCaseId(caseId);
        task.setSource(source == null ? "SCENE" : source);
        task.setDatabase(database == null || database.isBlank() ? "ALL" : database.toUpperCase());
        task.setKeywords(keywords);
        task.setOperatorId(user.getUserId());
        task.setTenantId(user.getTenantId());
        task.setCreateTime(LocalDateTime.now());
        task = taskRepository.save(task);

        // 候选生成(P1 降级: 线索词关键词检索; 无关键词则提示待补充)
        int candidates = generateCandidates(task, keywords, user);
        task.setStatus(candidates > 0 ? "REVIEWING" : "PENDING");
        taskRepository.save(task);
        log.info("[PHOTO-COMPARE] task {} created, {} candidates (alg=OCR_KM)", task.getId(), candidates);
        return task;
    }

    /** 候选生成: 线索词 → 实体检索 TopK → 布控命中标注 → 按比对库过滤 */
    private int generateCandidates(CompareTaskEntity task, String keywords, LoginUser user) {
        if (keywords == null || keywords.isBlank()) return 0;
        List<IntelligenceRes> hits = intelligenceService.search(keywords, null, user);
        int count = 0;
        for (IntelligenceRes hit : hits.stream().limit(DEFAULT_TOP_K).collect(Collectors.toList())) {
            if (!inDatabase(hit.getEntityId(), task.getDatabase(), user)) continue;
            CompareCandidateEntity c = new CompareCandidateEntity();
            c.setId(UUID.randomUUID().toString().replace("-", ""));
            c.setTaskId(task.getId());
            c.setEntityId(hit.getEntityId());
            // 相似度降级: 按排序位估算(真实特征比对在 P2/P3 替换)
            c.setSimilarity(Math.max(0.5, 0.9 - count * 0.08));
            // 布控命中标注(以图找人→布控联动)
            c.setWatchHit(!watchService.matchLists(hit.getEntityId(), task.getTenantId()).isEmpty());
            c.setStatus("PENDING");
            candidateRepository.save(c);
            count++;
        }
        return count;
    }

    public List<CompareTaskEntity> listTasks(String tenantId, String status) {
        return status != null && !status.isBlank()
                ? taskRepository.findByTenantIdAndStatusOrderByCreateTimeDesc(tenantId, status)
                : taskRepository.findByTenantIdOrderByCreateTimeDesc(tenantId);
    }

    public Map<String, Object> taskDetail(String taskId, LoginUser user) {
        CompareTaskEntity task = getTask(taskId, user.getTenantId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("task", task);
        out.put("candidates", candidateRepository.findByTaskIdOrderBySimilarityDesc(taskId).stream()
                .map(c -> candidateView(c, user))
                .collect(Collectors.toList()));
        return out;
    }

    /** 候选视图: 基础信息(密级脱敏) + 布控标注 */
    private Map<String, Object> candidateView(CompareCandidateEntity c, LoginUser user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("candidateId", c.getId());
        m.put("entityId", c.getEntityId());
        m.put("similarity", c.getSimilarity());
        m.put("watchHit", c.getWatchHit());
        m.put("status", c.getStatus());
        m.put("reason", c.getReason());
        try {
            var master = entityMasterService.detail(c.getEntityId(), user.getDensity());
            m.put("displayName", master.getDisplayName());
            m.put("entityType", master.getEntityType());
            m.put("primaryKey", master.getPrimaryKey());
            m.put("labels", master.getLabels());
        } catch (Exception e) {
            m.put("displayName", "未知实体");
        }
        return m;
    }

    /** 比对库过滤: WANTED(全国通缉标签)/RECORD(前科标签)/ARCHIVE(失踪标签); ALL 不过滤 */
    private boolean inDatabase(String entityId, String database, LoginUser user) {
        if (database == null || "ALL".equals(database)) return true;
        java.util.Set<String> codes = com.temporedata.guard.modules.police.entitymaster.repository.EntityLabelRepository.class.getName() == null
                ? java.util.Set.of() : entityLabels(entityId);
        if ("WANTED".equals(database)) return codes.contains("WANTED");
        if ("RECORD".equals(database)) return codes.contains("RECORD");
        if ("ARCHIVE".equals(database)) return codes.contains("ARCHIVE");
        return true;
    }

    private java.util.Set<String> entityLabels(String entityId) {
        return labelRepository.findByEntityId(entityId).stream()
                .map(l -> l.getLabelCode()).collect(Collectors.toSet());
    }

    // ==================== 人工复核闭环 ====================

    @Transactional
    public CompareCandidateEntity confirmCandidate(String taskId, String candidateId, LoginUser user) {
        CompareCandidateEntity c = getCandidate(taskId, candidateId, user.getTenantId());
        if (!"PENDING".equals(c.getStatus())) {
            throw new BusinessException("该候选已复核");
        }
        c.setStatus("CONFIRMED");
        c.setReviewerId(user.getUserId());
        c.setReviewTime(LocalDateTime.now());
        candidateRepository.save(c);
        finishIfAllReviewed(taskId);
        log.info("[PHOTO-COMPARE] candidate {} CONFIRMED by {}", candidateId, user.getUsername());
        return c;
    }

    @Transactional
    public CompareCandidateEntity excludeCandidate(String taskId, String candidateId, String reason, LoginUser user) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("排除必须填写原因(取证要求)");
        }
        CompareCandidateEntity c = getCandidate(taskId, candidateId, user.getTenantId());
        if (!"PENDING".equals(c.getStatus())) {
            throw new BusinessException("该候选已复核");
        }
        c.setStatus("EXCLUDED");
        c.setReason(reason);
        c.setReviewerId(user.getUserId());
        c.setReviewTime(LocalDateTime.now());
        candidateRepository.save(c);
        finishIfAllReviewed(taskId);
        log.info("[PHOTO-COMPARE] candidate {} EXCLUDED by {}: {}", candidateId, user.getUsername(), reason);
        return c;
    }

    /** 全部候选复核后任务闭环 */
    private void finishIfAllReviewed(String taskId) {
        List<CompareCandidateEntity> pending = candidateRepository.findByTaskIdAndStatus(taskId, "PENDING");
        if (pending.isEmpty()) {
            taskRepository.findById(taskId).ifPresent(t -> {
                t.setStatus("CONFIRMED");
                t.setFinishTime(LocalDateTime.now());
                taskRepository.save(t);
            });
        }
    }

    // ==================== 比对报告(取证) ====================

    @Transactional
    public CompareReportEntity generateReport(String taskId, LoginUser user) {
        CompareTaskEntity task = getTask(taskId, user.getTenantId());
        List<CompareCandidateEntity> candidates = candidateRepository.findByTaskIdOrderBySimilarityDesc(taskId);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("taskId", task.getId());
        report.put("operatorId", task.getOperatorId());
        report.put("operatorName", user.getUsername());
        report.put("fileId", task.getFileId());
        report.put("source", task.getSource());
        report.put("keywords", task.getKeywords());
        report.put("algorithm", task.getAlgorithm());
        report.put("createTime", task.getCreateTime() == null ? "" : task.getCreateTime().toString());
        report.put("finishTime", task.getFinishTime() == null ? "" : task.getFinishTime().toString());
        report.put("candidates", candidates.stream().map(c -> {
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("entityId", c.getEntityId());
            cm.put("similarity", c.getSimilarity());
            cm.put("watchHit", c.getWatchHit());
            cm.put("status", c.getStatus());
            cm.put("reason", c.getReason());
            cm.put("reviewerId", c.getReviewerId());
            cm.put("reviewTime", c.getReviewTime() == null ? "" : c.getReviewTime().toString());
            return cm;
        }).collect(Collectors.toList()));

        CompareReportEntity r = new CompareReportEntity();
        r.setId(UUID.randomUUID().toString().replace("-", ""));
        r.setTaskId(taskId);
        try {
            r.setReportContent(objectMapper.writeValueAsString(report));
        } catch (Exception e) {
            r.setReportContent(String.valueOf(report));
        }
        r.setCreatorId(user.getUserId());
        r.setCreateTime(LocalDateTime.now());
        return reportRepository.save(r);
    }

    public List<CompareReportEntity> listReports(String taskId, LoginUser user) {
        getTask(taskId, user.getTenantId());
        return reportRepository.findByTaskId(taskId);
    }

    // ==================== 私有 ====================

    private CompareTaskEntity getTask(String taskId, String tenantId) {
        CompareTaskEntity task = taskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("比对任务不存在"));
        if (!task.getTenantId().equals(tenantId)) {
            throw new BusinessException("无权访问该任务");
        }
        return task;
    }

    private CompareCandidateEntity getCandidate(String taskId, String candidateId, String tenantId) {
        getTask(taskId, tenantId);
        return candidateRepository.findById(candidateId)
                .filter(c -> c.getTaskId().equals(taskId))
                .orElseThrow(() -> new BusinessException("候选不存在"));
    }
}
