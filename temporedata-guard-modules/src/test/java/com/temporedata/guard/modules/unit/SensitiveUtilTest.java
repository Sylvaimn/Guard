package com.temporedata.guard.modules.unit;

import com.temporedata.guard.common.util.SensitiveUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** 脱敏工具单测: 主标识/姓名/手机/文本 */
class SensitiveUtilTest {

    @Test
    void maskKeyKeepsPrefixAndSuffix() {
        assertEquals("138****8888", SensitiveUtil.maskKey("13800008888"));
        assertNotEquals("13800008888", SensitiveUtil.maskKey("13800008888"));
    }

    @Test
    void maskPhoneKeepsFirst3Last4() {
        assertEquals("138****8888", SensitiveUtil.maskPhone("13800008888"));
    }

    @Test
    void maskNameMasksMiddle() {
        String masked = SensitiveUtil.maskName("张某某");
        assertNotNull(masked);
        assertFalse(masked.equals("张某某"));
    }

    @Test
    void maskTextHandlesEmbedded() {
        String out = SensitiveUtil.maskText("联系手机 13800008888");
        assertFalse(out.contains("13800008888"));
    }

    @Test
    void emptyOrNullSafe() {
        assertNull(SensitiveUtil.maskKey(null));
        assertEquals("", SensitiveUtil.maskKey(""));
    }
}
