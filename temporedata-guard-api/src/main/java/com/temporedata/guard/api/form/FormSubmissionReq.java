package com.temporedata.guard.api.form;

import lombok.Data;

/**
 * Form submission request.
 */
@Data
public class FormSubmissionReq {

    private String formId;

    private String data; // JSON submission data
}