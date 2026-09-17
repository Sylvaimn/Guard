package com.temporedata.guard.modules.police.photo.controller;
import com.temporedata.guard.modules.police.photo.entity.CompareCandidateEntity;
import com.temporedata.guard.modules.police.photo.entity.CompareReportEntity;
import com.temporedata.guard.modules.police.photo.entity.CompareTaskEntity;
import com.temporedata.guard.modules.police.photo.entity.PersonPhotoEntity;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.modules.police.photo.service.PhotoCompareService;
import com.temporedata.guard.modules.security.security.audit.Audit;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 以图找人（P1）：照片库 / 比对任务 / 人工复核 / 布控联动 / 比对报告。
 * 涉隐私操作全部 @Audit 留痕。
 */
@RestController
@RequestMapping("/api/photo")
@RequiredArgsConstructor
public class PhotoCompareController {

    private final PhotoCompareService photoCompareService;

    // ---- 照片库 ----

    @PostMapping("/persons/{entityId}/photos")
    @Audit(action = "CREATE", targetType = "PERSON_PHOTO", targetName = "#{#{#entityId}}", detail = "人员照片入库")
    public BaseResponse<PersonPhotoEntity> addPhoto(@PathVariable String entityId,
                                                    @RequestBody Map<String, String> body,
                                                    @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.addPersonPhoto(
                entityId, body.get("fileId"), body.getOrDefault("photoType", "SCENE"), user));
    }

    @GetMapping("/persons/{entityId}/photos")
    public BaseResponse<List<PersonPhotoEntity>> listPhotos(@PathVariable String entityId,
                                                            @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.listPersonPhotos(entityId, user));
    }

    // ---- 比对任务 ----

    @PostMapping("/compare")
    @Audit(action = "CREATE", targetType = "PHOTO_COMPARE", detail = "以图找人: 照片 #{#body?.get('fileId')} 线索 #{#body?.get('keywords')}")
    public BaseResponse<CompareTaskEntity> compare(@RequestBody Map<String, String> body,
                                                   @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.createTask(
                body.get("fileId"), body.get("keywords"), body.get("caseId"), body.get("source"),
                body.get("database"), user));
    }

    @GetMapping("/tasks")
    public BaseResponse<List<CompareTaskEntity>> tasks(@RequestParam(required = false) String status,
                                                       @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.listTasks(user.getTenantId(), status));
    }

    @GetMapping("/tasks/{id}")
    public BaseResponse<Map<String, Object>> taskDetail(@PathVariable String id,
                                                        @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.taskDetail(id, user));
    }

    // ---- 人工复核(涉隐私, 必留痕) ----

    @PostMapping("/tasks/{id}/candidates/{cid}/confirm")
    @Audit(action = "UPDATE", targetType = "PHOTO_COMPARE", targetName = "#{#{#id}}", detail = "确认候选 #{#{#cid}}")
    public BaseResponse<CompareCandidateEntity> confirm(@PathVariable String id, @PathVariable String cid,
                                                        @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.confirmCandidate(id, cid, user));
    }

    @PostMapping("/tasks/{id}/candidates/{cid}/exclude")
    @Audit(action = "UPDATE", targetType = "PHOTO_COMPARE", targetName = "#{#{#id}}", detail = "排除候选 #{#{#cid}} 原因: #{#body?.get('reason')}")
    public BaseResponse<CompareCandidateEntity> exclude(@PathVariable String id, @PathVariable String cid,
                                                        @RequestBody Map<String, String> body,
                                                        @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.excludeCandidate(id, cid, body.get("reason"), user));
    }

    // ---- 比对报告(取证) ----

    @PostMapping("/tasks/{id}/report")
    @Audit(action = "CREATE", targetType = "PHOTO_COMPARE_REPORT", targetName = "#{#{#id}}", detail = "生成比对报告")
    public BaseResponse<CompareReportEntity> report(@PathVariable String id, @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.generateReport(id, user));
    }

    @GetMapping("/tasks/{id}/reports")
    public BaseResponse<List<CompareReportEntity>> reports(@PathVariable String id,
                                                           @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(photoCompareService.listReports(id, user));
    }
}
