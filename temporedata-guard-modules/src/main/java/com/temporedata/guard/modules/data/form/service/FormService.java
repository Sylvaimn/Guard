package com.temporedata.guard.modules.data.form.service;
import com.temporedata.guard.modules.data.form.entity.FormEntity;
import com.temporedata.guard.modules.data.form.repository.FormRepository;
import com.temporedata.guard.modules.data.form.entity.FormSubmissionEntity;
import com.temporedata.guard.modules.data.form.repository.FormSubmissionRepository;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.form.FormReq;
import com.temporedata.guard.api.form.FormRes;
import com.temporedata.guard.api.form.FormSubmissionReq;
import com.temporedata.guard.api.form.FormSubmissionRes;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Form management service: CRUD, submissions, share token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FormService {

    private final FormRepository formRepository;
    private final FormSubmissionRepository formSubmissionRepository;

    // ---- Form CRUD ----

    public List<FormRes> listAll(String tenantId) {
        return formRepository.findByTenantIdOrderByCreateTimeDesc(tenantId).stream()
                .map(this::toRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public FormRes create(FormReq req, String tenantId) {
        FormEntity entity = new FormEntity();
        entity.setId(UUID.randomUUID().toString().replace("-", ""));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setConfig(req.getConfig());
        entity.setCategory(req.getCategory());
        entity.setSensitive(req.getSensitive() != null && req.getSensitive());
        entity.setStatus("DRAFT");
        entity.setTenantId(tenantId);
        entity.setCreateTime(LocalDateTime.now());
        return toRes(formRepository.save(entity));
    }

    @Transactional
    public FormRes update(String id, FormReq req) {
        FormEntity entity = formRepository.findById(id)
                .orElseThrow(() -> new BusinessException("表单不存在"));
        entity.setName(req.getName());
        entity.setDescription(req.getDescription());
        entity.setConfig(req.getConfig());
        entity.setCategory(req.getCategory());
        entity.setSensitive(req.getSensitive());
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(formRepository.save(entity));
    }

    @Transactional
    public void delete(String id) {
        formSubmissionRepository.findByFormId(id).forEach(s -> formSubmissionRepository.deleteById(s.getId()));
        formRepository.deleteById(id);
    }

    // ---- Submissions ----

    public List<FormSubmissionRes> listSubmissions(String formId) {
        return formSubmissionRepository.findByFormId(formId).stream()
                .map(this::toSubmissionRes)
                .collect(Collectors.toList());
    }

    @Transactional
    public FormSubmissionRes submitForm(String formId, String data, String ip) {
        FormEntity form = formRepository.findById(formId)
                .orElseThrow(() -> new BusinessException("表单不存在"));
        FormSubmissionEntity submission = new FormSubmissionEntity();
        submission.setId(UUID.randomUUID().toString().replace("-", ""));
        submission.setFormId(formId);
        submission.setData(data);
        submission.setSubmitterIp(ip);
        submission.setTenantId(form.getTenantId());
        submission.setSubmitTime(LocalDateTime.now());
        return toSubmissionRes(formSubmissionRepository.save(submission));
    }

    // ---- Share Token ----

    @Transactional
    public FormRes generateShareToken(String formId) {
        FormEntity entity = formRepository.findById(formId)
                .orElseThrow(() -> new BusinessException("表单不存在"));
        entity.setShareToken(UUID.randomUUID().toString().replace("-", ""));
        entity.setStatus("PUBLISHED");
        entity.setUpdateTime(LocalDateTime.now());
        return toRes(formRepository.save(entity));
    }

    public FormRes getByShareToken(String shareToken) {
        FormEntity entity = formRepository.findByShareToken(shareToken);
        if (entity == null) {
            throw new BusinessException("分享链接无效");
        }
        return toRes(entity);
    }

    @Transactional
    public FormSubmissionRes submitByShareToken(String shareToken, String data, String ip) {
        FormEntity form = formRepository.findByShareToken(shareToken);
        if (form == null) {
            throw new BusinessException("分享链接无效");
        }
        return submitForm(form.getId(), data, ip);
    }

    // ---- Mappers ----

    private FormRes toRes(FormEntity e) {
        FormRes res = new FormRes();
        res.setId(e.getId());
        res.setName(e.getName());
        res.setDescription(e.getDescription());
        res.setConfig(e.getConfig());
        res.setStatus(e.getStatus());
        res.setShareToken(e.getShareToken());
        res.setTenantId(e.getTenantId());
        res.setCreateTime(e.getCreateTime() != null ? e.getCreateTime().toString() : null);
        res.setUpdateTime(e.getUpdateTime() != null ? e.getUpdateTime().toString() : null);
        return res;
    }

    private FormSubmissionRes toSubmissionRes(FormSubmissionEntity e) {
        FormSubmissionRes res = new FormSubmissionRes();
        res.setId(e.getId());
        res.setFormId(e.getFormId());
        res.setData(e.getData());
        res.setSubmitterIp(e.getSubmitterIp());
        res.setTenantId(e.getTenantId());
        res.setSubmitTime(e.getSubmitTime() != null ? e.getSubmitTime().toString() : null);
        return res;
    }
}