package com.temporedata.guard.api.indicator;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Paged indicator list response.
 */
@Data
public class IndicatorPageRes {

    private long total;

    private List<IndicatorRes> records = new ArrayList<>();
}