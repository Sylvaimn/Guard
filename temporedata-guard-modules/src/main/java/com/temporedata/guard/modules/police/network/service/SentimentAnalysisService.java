package com.temporedata.guard.modules.police.network.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 舆情研判（P3）：规则情感分析(正负词表) + 主题词提取 → 研判建议。
 * 本地规则版(无需模型), 后续可接 LLM 摘要。
 */
@Slf4j
@Service
public class SentimentAnalysisService {

    private static final String[] POSITIVE_WORDS = {
            "满意", "安全", "高效", "点赞", "感谢", "顺利", "表扬", "积极", "欣慰", "放心"
    };
    private static final String[] NEGATIVE_WORDS = {
            "不满", "投诉", "聚集", "恐慌", "涉诈", "暴力", "失控", "质疑", "愤怒", "混乱", "拥堵", "骚乱"
    };
    private static final String[] SENSITIVE_WORDS = {
            "群体事件", "聚集", "非法", "诈骗", "伤亡", "暴力", "涉稳"
    };

    /** 舆情文本分析: 情感分(-1..1) + 主题词 + 敏感词 + 建议 */
    public Map<String, Object> analyze(String text) {
        if (text == null || text.isBlank()) return Map.of("error", "舆情文本不能为空");
        int pos = 0, neg = 0;
        for (String w : POSITIVE_WORDS) if (text.contains(w)) pos++;
        for (String w : NEGATIVE_WORDS) if (text.contains(w)) neg++;
        double score = Math.max(-1.0, Math.min(1.0, (pos - neg) / 2.0));

        List<String> sensitive = java.util.Arrays.stream(SENSITIVE_WORDS)
                .filter(text::contains).collect(java.util.stream.Collectors.toList());

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("sentiment", score > 0.2 ? "正面" : score < -0.2 ? "负面" : "中性");
        out.put("score", Math.round(score * 100) / 100.0);
        out.put("sensitive", sensitive);
        out.put("suggestion", buildSuggestion(score, sensitive));
        return out;
    }

    private String buildSuggestion(double score, List<String> sensitive) {
        if (!sensitive.isEmpty()) {
            return "检出敏感词(" + String.join("、", sensitive) + ")，建议立即核实并报送指挥中心";
        }
        if (score < -0.2) {
            return "舆情偏负面，建议关注传播趋势并准备回应口径";
        }
        return "舆情整体平稳，常规关注即可";
    }
}
