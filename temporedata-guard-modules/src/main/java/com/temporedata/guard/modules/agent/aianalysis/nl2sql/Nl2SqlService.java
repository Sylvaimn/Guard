/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.temporedata.guard.api.base.exceptions.BusinessException
 *  com.temporedata.guard.api.query.QueryReq
 *  com.temporedata.guard.api.query.QueryRes
 *  com.temporedata.guard.security.LoginUser
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.stereotype.Service
 */
package com.temporedata.guard.modules.agent.aianalysis.nl2sql;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.api.query.QueryReq;
import com.temporedata.guard.api.query.QueryRes;
import com.temporedata.guard.modules.agent.agent.service.AiModelGateway;
import com.temporedata.guard.modules.dev.catalog.entity.TableMetadataEntity;
import com.temporedata.guard.modules.dev.catalog.repository.TableMetadataRepository;
import com.temporedata.guard.modules.dev.query.service.QueryService;
import com.temporedata.guard.security.LoginUser;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class Nl2SqlService {
    private static final Logger log = LoggerFactory.getLogger(Nl2SqlService.class);
    private final AiModelGateway aiModelGateway;
    private final TableMetadataRepository tableMetadataRepository;
    private final QueryService queryService;
    private static final Pattern SELECT_ONLY = Pattern.compile("^\\s*(select|with)\\b.*", 34);

    public Map<String, Object> ask(String datasourceId, String question, LoginUser user) {
        if (question == null || question.isBlank()) {
            throw new BusinessException("\u95ee\u9898\u4e0d\u80fd\u4e3a\u7a7a");
        }
        List<TableMetadataEntity> tables = this.tableMetadataRepository.findByDatasourceId(datasourceId);
        if (tables.isEmpty()) {
            throw new BusinessException("\u8be5\u6570\u636e\u6e90\u5c1a\u672a\u540c\u6b65\u8868\u7ed3\u6784\uff0c\u8bf7\u5148\u5728\u6570\u636e\u8d44\u4ea7\u4e2d\u540c\u6b65");
        }
        String metadata = tables.stream().map(t -> "- " + t.getTableName() + " (" + (t.getComment() != null ? t.getComment() : "") + " \u884c\u6570\u2248" + (Serializable)(t.getRowCount() == null ? "?" : t.getRowCount()) + ")").collect(Collectors.joining("\n"));
        String sql = null;
        try {
            String reply = this.aiModelGateway.chat(user.getTenantId(), user.getDensity(), "nl2sql", List.of(Map.of("role", "system", "content", "\u4f60\u662f\u6570\u636e\u95ee\u7b54\u52a9\u624b\u3002\u4ec5\u8f93\u51fa\u4e00\u6761 SELECT SQL\uff08\u4e0d\u8981\u4efb\u4f55\u89e3\u91ca/\u6ce8\u91ca/\u5206\u53f7\u7ed3\u5c3e\uff09\uff0c\u57fa\u4e8e\u4ee5\u4e0b\u8868\u7ed3\u6784\u56de\u7b54\u3002\u82e5\u65e0\u6cd5\u56de\u7b54\u8f93\u51fa NONE\u3002\n\u8868\u7ed3\u6784\uff1a\n" + metadata), Map.of("role", "user", "content", question)));
            if (reply != null && !reply.isBlank() && !reply.contains("NONE")) {
                sql = reply.trim().replaceAll(";\\s*$", "");
                sql = sql.replaceAll("```sql|```", "").trim();
            }
        }
        catch (Exception e) {
            log.warn("NL2SQL generation failed: {}", (Object)e.getMessage());
        }
        LinkedHashMap<String, Object> out = new LinkedHashMap<String, Object>();
        out.put("tables", tables.size());
        if (sql == null) {
            out.put("sql", null);
            out.put("reply", "\u5f53\u524d\u672a\u914d\u7f6e\u6a21\u578b\u6216\u6d89\u5bc6\u9650\u5236\uff0c\u5df2\u8fd4\u56de\u6570\u636e\u6e90\u8868\u6e05\u5355\u4f9b\u4eba\u5de5\u68c0\u7d22\uff1a" + metadata);
            return out;
        }
        if (!SELECT_ONLY.matcher(sql).matches()) {
            throw new BusinessException("\u6a21\u578b\u751f\u6210\u7684\u975e\u67e5\u8be2\u8bed\u53e5\u5df2\u88ab\u62e6\u622a: " + sql);
        }
        QueryReq req = new QueryReq();
        req.setDatasourceId(datasourceId);
        req.setSql(sql);
        QueryRes result = this.queryService.execute(req, user.getUserId(), user.getTenantId());
        out.put("sql", sql);
        out.put("columns", result.getColumns());
        out.put("rows", result.getRows());
        out.put("rowCount", result.getRowCount());
        out.put("chart", this.inferChart(result.getColumns(), result.getRows()));
        out.put("reply", "\u5df2\u6267\u884c\u67e5\u8be2\uff0c\u5171 " + result.getRowCount() + " \u884c\u7ed3\u679c\u3002\u8bf7\u6838\u5bf9 SQL \u4e0e\u6570\u636e\u540e\u4f7f\u7528\u3002");
        return out;
    }

    private Map<String, Object> inferChart(List<String> columns, List<?> rows) {
        boolean lastNumeric;
        LinkedHashMap<String, Object> chart = new LinkedHashMap<String, Object>();
        chart.put("type", "table");
        if (columns == null || rows == null || rows.isEmpty() || columns.size() < 2) {
            return chart;
        }
        boolean bl = lastNumeric = rows.get(0) instanceof List && ((List)rows.get(0)).size() == columns.size() && ((List)rows.get(0)).get(columns.size() - 1) instanceof Number;
        if (!lastNumeric) {
            return chart;
        }
        String lastCol = columns.get(columns.size() - 1);
        String firstCol = columns.get(0);
        if (firstCol.toLowerCase().contains("time") || firstCol.toLowerCase().contains("date") || firstCol.toLowerCase().contains("\u6708") || firstCol.toLowerCase().contains("\u65e5")) {
            chart.put("type", "line");
        } else if (columns.size() == 2) {
            chart.put("type", "pie");
        } else {
            chart.put("type", "bar");
        }
        chart.put("category", firstCol);
        chart.put("value", lastCol);
        return chart;
    }

    public Nl2SqlService(AiModelGateway aiModelGateway, TableMetadataRepository tableMetadataRepository, QueryService queryService) {
        this.aiModelGateway = aiModelGateway;
        this.tableMetadataRepository = tableMetadataRepository;
        this.queryService = queryService;
    }
}

