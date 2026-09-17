package com.temporedata.guard.api.license;

import lombok.Data;

/**
 * License response.
 */
@Data
public class LicenseRes {

    private String id;

    private String licenseKey;

    private String status;

    private String expireDate;

    private String installDate;
}