package com.temporedata.guard.modules.common.file.controller;

import com.temporedata.guard.api.base.pojos.BaseResponse;
import com.temporedata.guard.api.file.FileRes;
import com.temporedata.guard.modules.common.file.service.FileService;
import com.temporedata.guard.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    public BaseResponse<FileRes> upload(@RequestParam("file") MultipartFile file,
                                         @AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(fileService.upload(file, user.getTenantId()));
    }

    @GetMapping
    public BaseResponse<List<FileRes>> list(@AuthenticationPrincipal LoginUser user) {
        return BaseResponse.success(fileService.list(user.getTenantId()));
    }

    @GetMapping("/{id}/preview")
    public BaseResponse<java.util.Map<String, Object>> preview(@PathVariable String id) {
        return BaseResponse.success(fileService.preview(id));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable String id) {
        Resource resource = fileService.download(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable String id) {
        fileService.delete(id);
        return BaseResponse.success();
    }
}