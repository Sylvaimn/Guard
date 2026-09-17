package com.temporedata.guard.modules.common.file.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.file.FileRes;
import com.temporedata.guard.modules.common.file.entity.FileEntity;
import com.temporedata.guard.modules.common.file.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.temporedata.guard.modules.common.storage.StorageService;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;
    private final com.temporedata.guard.modules.dev.lineage.service.LineageService lineageService;
    private final com.temporedata.guard.modules.common.resource.service.ResourceService resourceService;

    private static final Path UPLOAD_DIR = Paths.get("./data/files").toAbsolutePath().normalize();

    @Transactional
    public FileRes upload(MultipartFile file, String tenantId) {
        try {
            Files.createDirectories(UPLOAD_DIR);
        } catch (IOException e) {
            throw new BusinessException("创建上传目录失败");
        }

        String originalName = file.getOriginalFilename();
        String storedName = UUID.randomUUID().toString().replace("-", "") + "_" + originalName;
        String key = "upload/" + storedName;

        try {
            storageService.store(key, file.getInputStream(), file.getSize());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");
        }

        FileEntity entity = new FileEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(storedName);
        entity.setOriginalName(originalName);
        entity.setFilePath(key);
        entity.setFileSize(file.getSize());
        entity.setFileType(file.getContentType());
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());

        FileRes res = toRes(fileRepository.save(entity));
        try {
            autoRegisterLineage(entity, file);
        } catch (Exception e) {
            log.warn("自动血缘采集失败: {} - {}", entity.getOriginalName(), e.getMessage());
        }
        return res;
    }

    /** 已知表列映射(元数据未采集时的表头推断兜底) */
    private static final java.util.Map<String, String[]> KNOWN_COLUMNS = java.util.Map.of(
            "track_flow_daily", new String[]{"region", "stat_date", "renci", "guiji"},
            "track_point_partitioned", new String[]{"region", "stat_date", "lon", "lat", "count"},
            "case_stats_daily", new String[]{"case_id", "case_type", "stat_date", "amount"},
            "fund_flow_stats", new String[]{"fund_id", "from_account", "to_account", "amount"},
            "watch_alert_daily", new String[]{"alert_id", "target_name", "alert_time", "level"});

    /**
     * 上传即血缘: 解析文件结构(文件名/表头)推断目标表, 自动登记 FILE->TABLE LOAD 边。
     * 无需手动登记、无需表已生成。
     */
    private void autoRegisterLineage(FileEntity entity, MultipartFile file) {
        String original = entity.getOriginalName() == null ? "" : entity.getOriginalName();
        if (original.isBlank()) return;
        String table = inferTargetTable(original, file);
        if (table == null || table.isBlank()) {
            // 未命中表: 仅建 FILE 节点(血缘图可见, 可后续手动登记)
            lineageService.collectResourceLineage(entity.getTenantId(), entity.getId(),
                    entity.getOriginalName(), null, "LOAD");
            return;
        }
        lineageService.collectResourceLineage(entity.getTenantId(), entity.getId(),
                entity.getOriginalName(), table, "LOAD");
        try {
            resourceService.createFromFile(entity.getId(), entity.getOriginalName(), "csv",
                    entity.getFilePath(), entity.getFileSize(), entity.getTenantId(), table, "LOAD");
        } catch (Exception ignored) {
        }
    }

    /** 推断目标表: ①文件名匹配(统一目录/已知表) ②CSV/JSON 表头匹配 */
    private String inferTargetTable(String originalName, MultipartFile file) {
        String base = originalName.toLowerCase();
        String stem = base.contains(".") ? base.substring(0, base.lastIndexOf('.')) : base;
        stem = stem.replaceAll("[^a-z0-9_]", "_").replaceAll("_+", "_").replaceAll("^_|_$", "");

        // ① 文件名匹配: 统一目录 + 已知表
        try {
            java.util.List<String> known = jdbcTemplate.queryForList(
                    "SELECT DISTINCT item_name FROM temporedata_guard_meta_unified WHERE item_name IS NOT NULL",
                    String.class);
            known.addAll(KNOWN_COLUMNS.keySet());
            for (String t : known) {
                String tn = String.valueOf(t).toLowerCase();
                if (stem.equals(tn) || stem.endsWith("_" + tn) || stem.contains(tn)) {
                    return String.valueOf(t);
                }
            }
        } catch (Exception ignored) {
        }

        // ② 表头匹配(CSV 前 100 行内找第一行非空, 与已知表列 ≥2 列命中)
        try {
            String head = new String(file.getInputStream().readNBytes(8192), java.nio.charset.StandardCharsets.UTF_8);
            String firstLine = java.util.Arrays.stream(head.split("\n"))
                    .map(String::trim).filter(l -> !l.isBlank()).findFirst().orElse("");
            if (firstLine.isBlank()) return null;
            String[] cols = firstLine.split("[,;\t]");
            int best = 0;
            String bestTable = null;
            for (java.util.Map.Entry<String, String[]> e : KNOWN_COLUMNS.entrySet()) {
                int hit = 0;
                for (String c : cols) {
                    for (String k : e.getValue()) {
                        if (c.trim().equalsIgnoreCase(k)) { hit++; break; }
                    }
                }
                if (hit >= 2 && hit > best) {
                    best = hit;
                    bestTable = e.getKey();
                }
            }
            if (bestTable != null) return bestTable;
        } catch (Exception ignored) {
        }
        return null;
    }

    public List<FileRes> list(String tenantId) {
        return fileRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(String id) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        try {
            storageService.delete(entity.getFilePath());
        } catch (Exception e) {
            log.warn("删除物理文件失败: {}", entity.getFilePath(), e);
        }
        fileRepository.deleteById(id);
    }

    public Resource download(String id) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        String key = entity.getFilePath();
        // 兼容历史记录(绝对路径存储): 本地回退
        if (key != null && key.startsWith("/")) {
            Path filePath = Paths.get(key);
            if (Files.exists(filePath)) {
                try {
                    return new UrlResource(filePath.toUri());
                } catch (MalformedURLException e) {
                    throw new BusinessException("文件读取失败");
                }
            }
        }
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            if (key == null || !storageService.read(key, bos)) {
                throw new BusinessException("文件不存在");
            }
            return new InputStreamResource(new java.io.ByteArrayInputStream(bos.toByteArray()));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件读取失败");
        }
    }

    /**
     * 文件预览: 按格式返回可渲染内容。
     * 文本类(csv/txt/json/log/xml) -> 原文; 图片/PDF -> base64; 其他 -> binary 提示下载。
     */
    public java.util.Map<String, Object> preview(String id) {
        FileEntity entity = fileRepository.findById(id)
                .orElseThrow(() -> new BusinessException("文件不存在"));
        byte[] bytes;
        try {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            if (entity.getFilePath() == null || !storageService.read(entity.getFilePath(), bos)) {
                throw new BusinessException("文件不存在");
            }
            bytes = bos.toByteArray();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("文件读取失败");
        }
        String name = entity.getOriginalName() == null ? "" : entity.getOriginalName().toLowerCase();
        String ext = name.contains(".") ? name.substring(name.lastIndexOf(".") + 1) : "";
        java.util.Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("id", entity.getId());
        out.put("name", entity.getOriginalName());
        out.put("ext", ext);
        if (java.util.Set.of("csv", "txt", "json", "log", "xml", "sql", "md", "yml", "yaml").contains(ext)) {
            out.put("type", "text");
            out.put("content", new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
        } else if (java.util.Set.of("png", "jpg", "jpeg", "gif", "svg", "webp", "bmp").contains(ext)) {
            out.put("type", "image");
            out.put("content", "data:image/" + ("jpg".equals(ext) ? "jpeg" : ext) + ";base64,"
                    + java.util.Base64.getEncoder().encodeToString(bytes));
        } else if ("pdf".equals(ext)) {
            out.put("type", "pdf");
            out.put("content", "data:application/pdf;base64," + java.util.Base64.getEncoder().encodeToString(bytes));
        } else if (java.util.Set.of("xls", "xlsx", "zip", "jar", "tar.gz", "gz", "parquet", "avro", "doc", "docx").contains(ext)) {
            out.put("type", "binary");
            out.put("size", bytes.length);
        } else {
            out.put("type", "binary");
            out.put("size", bytes.length);
        }
        return out;
    }

    private FileRes toRes(FileEntity e) {
        FileRes res = new FileRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setOriginalName(e.getOriginalName());
        res.setFileSize(e.getFileSize());
        res.setFileType(e.getFileType());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        return res;
    }
}