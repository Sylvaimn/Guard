package com.temporedata.guard.api.preference;

import lombok.Data;

/**
 * Preference response.
 */
@Data
public class PreferenceRes {

    private String id;

    private String userId;

    private String prefKey;

    private String prefValue;

    private String createTime;

    private String updateTime;
}