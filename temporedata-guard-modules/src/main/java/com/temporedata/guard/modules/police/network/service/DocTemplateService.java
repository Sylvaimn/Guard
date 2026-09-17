package com.temporedata.guard.modules.police.network.service;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 执法文书生成（P3）：模板 + 事实变量填充 → 文书草稿(人工修订后使用)。
 */
@Slf4j
@Service
public class DocTemplateService {

    /** 模板库 */
    private static final Map<String, String> TEMPLATES = new LinkedHashMap<>();

    static {
        TEMPLATES.put("询问笔录", "询问笔录\n\n时间：{date}\n地点：{place}\n询问人：{operator}\n被询问人：{person}\n\n问：请你陈述{caseName}相关情况。\n答：{statement}\n\n以上笔录经本人核对无误。");
        TEMPLATES.put("立案报告", "立案报告\n\n案件名称：{caseName}\n案件来源：{source}\n报案人：{reporter}\n简要案情：{summary}\n\n经审查，符合立案条件，建议立案侦查。\n\n承办人：{operator}\n{date}");
        TEMPLATES.put("比对报告", "照片比对报告\n\n比对照片：{photoRef}\n比对结果：{result}\n相似人员：{person}\n\n经办人：{operator}\n{date}");
    }

    /** 生成文书: 模板名 + 变量 → 填充文本 */
    public Map<String, Object> generate(String templateName, Map<String, String> variables) {
        String template = TEMPLATES.get(templateName);
        if (template == null) {
            throw new BusinessException("未知文书模板: " + templateName + "（可用: " + String.join("/", TEMPLATES.keySet()) + "）");
        }
        String content = template;
        for (Map.Entry<String, String> e : variables.entrySet()) {
            content = content.replace("{" + e.getKey() + "}", e.getValue() == null ? "" : e.getValue());
        }
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm"));
        content = content.replace("{date}", date);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("template", templateName);
        out.put("content", content);
        out.put("note", "AI 生成文书草稿，请民警核对修订后使用");
        return out;
    }

    public Map<String, String> templates() {
        return TEMPLATES;
    }
}
