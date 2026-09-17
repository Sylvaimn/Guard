package com.temporedata.guard.common.util;

/**
 * 敏感信息脱敏工具（警用数据最小化展示）。
 * 与情报检索 maskKey 统一规则：身份证/车牌等主标识保留前3后4。
 */
public final class SensitiveUtil {

    private SensitiveUtil() {
    }

    /** 主标识脱敏: 身份证号/车牌号/IMEI 等, 短值原样返回 */
    public static String maskKey(String key) {
        if (key == null || key.length() < 8) return key;
        return key.substring(0, 3) + "****" + key.substring(key.length() - 4);
    }

    /** 姓名脱敏: 张三 -> 张* */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) return name;
        if (name.length() == 1) return name;
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /** 手机号: 138****8000 */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }

    /** 通用: 中间隐藏 */
    public static String maskMiddle(String value, int keepPrefix, int keepSuffix) {
        if (value == null || value.length() <= keepPrefix + keepSuffix) return value;
        return value.substring(0, keepPrefix) + "*".repeat(value.length() - keepPrefix - keepSuffix)
                + value.substring(value.length() - keepSuffix);
    }

    private static final java.util.regex.Pattern ID_CARD =
            java.util.regex.Pattern.compile("(?<!\\d)\\d{17}[0-9Xx](?!\\d)");
    private static final java.util.regex.Pattern PHONE =
            java.util.regex.Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");
    private static final java.util.regex.Pattern BANK_CARD =
            java.util.regex.Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

    /** 文本脱敏: 扫描文本中的身份证号/手机号/银行卡并打码(Agent 回复/报表摘要等出口统一调用) */
    public static String maskText(String text) {
        if (text == null || text.isEmpty()) return text;
        String t = ID_CARD.matcher(text).replaceAll(m -> maskMiddle(m.group(), 3, 4));
        t = PHONE.matcher(t).replaceAll(m -> maskMiddle(m.group(), 3, 4));
        t = BANK_CARD.matcher(t).replaceAll(m -> maskMiddle(m.group(), 4, 4));
        return t;
    }
}
