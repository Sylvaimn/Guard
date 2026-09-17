package com.temporedata.guard.api.license;

import lombok.Data;

/**
 * Install license request.
 */
@Data
public class LicenseInstallReq {

    private String licenseKey;
}