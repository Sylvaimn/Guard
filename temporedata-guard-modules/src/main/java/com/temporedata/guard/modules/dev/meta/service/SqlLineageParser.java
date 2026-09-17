package com.temporedata.guard.modules.dev.meta.service;

import com.temporedata.guard.api.meta.SqlLineageRes;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.LateralSubSelect;
import net.sf.jsqlparser.statement.select.ParenthesedFromItem;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.select.TableFunction;
import net.sf.jsqlparser.statement.select.WithItem;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SQL lineage parser built on JSQLParser 4.8.
 * Extracts table-level and field-level lineage from INSERT ... SELECT / CREATE TABLE AS SELECT / SELECT.
 */
@Slf4j
@Component
public class SqlLineageParser {

    private static class ParseContext {
        final Map<String, String> aliasToTable = new HashMap<>();
        final Map<String, List<String>> virtualCols = new HashMap<>();
        final Map<String, List<String>> virtualExprs = new HashMap<>();
    }

    /**
     * Parse a SQL statement and extract lineage.
     *
     * @return result with output table, input tables, field mappings.
     *         Returns null if statement is not analysable.
     */
    public SqlLineageRes parse(String sql) {
        if (sql == null || sql.trim().isEmpty()) return null;

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(sql);
        } catch (JSQLParserException e) {
            log.warn("SQL parse failed: {}", e.getMessage());
            return null;
        }

        ParseContext ctx = new ParseContext();
        if (statement instanceof Insert) {
            return parseInsert((Insert) statement, ctx);
        }
        if (statement instanceof CreateTable) {
            return parseCreateTable((CreateTable) statement, ctx);
        }
        if (statement instanceof Select) {
            return parseSelect((Select) statement, ctx);
        }
        return null;
    }

    private SqlLineageRes parseInsert(Insert insert, ParseContext ctx) {
        Table target = insert.getTable();
        if (target == null) return null;

        List<String> targetColumns = new ArrayList<>();
        if (insert.getColumns() != null) {
            for (Column c : insert.getColumns()) {
                targetColumns.add(normalize(c.getColumnName()));
            }
        }

        Select select = insert.getSelect();
        if (select == null) return null;

        SqlLineageRes res = new SqlLineageRes();
        res.setSqlType("INSERT_SELECT");
        res.setOutputTable(normalize(target.getName()));

        Map<String, String> inputAliases = new LinkedHashMap<>();
        gatherSelectTables(select, inputAliases, ctx);
        res.setInputTables(new ArrayList<>(inputAliases.keySet()));

        List<SqlLineageRes.FieldMapping> mappings = extractFieldMappings(select, targetColumns, inputAliases, ctx);
        res.setFieldMappings(mappings);
        return res;
    }

    private SqlLineageRes parseCreateTable(CreateTable createTable, ParseContext ctx) {
        Table target = createTable.getTable();
        if (target == null) return null;

        Select select = createTable.getSelect();
        if (select == null) return null;

        SqlLineageRes res = new SqlLineageRes();
        res.setSqlType("CTAS");
        res.setOutputTable(normalize(target.getName()));

        Map<String, String> inputAliases = new LinkedHashMap<>();
        gatherSelectTables(select, inputAliases, ctx);
        res.setInputTables(new ArrayList<>(inputAliases.keySet()));

        List<SqlLineageRes.FieldMapping> mappings = extractFieldMappings(select, null, inputAliases, ctx);
        res.setFieldMappings(mappings);
        return res;
    }

    private SqlLineageRes parseSelect(Select select, ParseContext ctx) {
        SqlLineageRes res = new SqlLineageRes();
        res.setSqlType("SELECT");
        res.setOutputTable(null);

        Map<String, String> inputAliases = new LinkedHashMap<>();
        gatherSelectTables(select, inputAliases, ctx);
        res.setInputTables(new ArrayList<>(inputAliases.keySet()));
        return res;
    }

    /**
     * Recursively collect tables referenced in the SELECT (FROM / JOIN / subqueries / CTEs).
     */
    private void gatherSelectTables(Select select, Map<String, String> aliases, ParseContext ctx) {
        if (select.getWithItemsList() != null) {
            for (WithItem with : select.getWithItemsList()) {
                if (with.getSelect() != null) {
                    gatherSelectTables(with.getSelect(), aliases, ctx);
                }
            }
        }
        gatherSelectBody(select, aliases, ctx);
    }

    private void gatherSelectBody(Select select, Map<String, String> aliases, ParseContext ctx) {
        if (select instanceof PlainSelect) {
            PlainSelect ps = (PlainSelect) select;
            if (ps.getFromItem() != null) {
                gatherFromItem(ps.getFromItem(), aliases, ctx);
            }
            if (ps.getJoins() != null) {
                for (Join join : ps.getJoins()) {
                    if (join.getRightItem() != null) {
                        gatherFromItem(join.getRightItem(), aliases, ctx);
                    }
                }
            }
        } else if (select instanceof SetOperationList) {
            for (Select s : ((SetOperationList) select).getSelects()) {
                gatherSelectBody(s, aliases, ctx);
            }
        } else if (select instanceof ParenthesedSelect) {
            Select inner = ((ParenthesedSelect) select).getSelect();
            if (inner != null) {
                gatherSelectBody(inner, aliases, ctx);
            }
        }
    }

    private void gatherFromItem(FromItem fromItem, Map<String, String> aliases, ParseContext ctx) {
        if (fromItem instanceof Table) {
            Table t = (Table) fromItem;
            String name = normalize(t.getName());
            String alias = t.getAlias() != null ? t.getAlias().getName() : null;
            if (alias == null) alias = name;
            aliases.putIfAbsent(name, alias);
            ctx.aliasToTable.putIfAbsent(alias.toUpperCase(), name);
        } else if (fromItem instanceof ParenthesedSelect) {
            ParenthesedSelect sub = (ParenthesedSelect) fromItem;
            String alias = sub.getAlias() != null ? sub.getAlias().getName() : null;
            if (sub.getSelect() != null) {
                gatherSelectBody(sub.getSelect(), aliases, ctx);
            }
            if (alias != null) {
                // Virtual source: resolve columns from its select items
                registerVirtualAlias(alias, sub, ctx);
            }
        } else if (fromItem instanceof ParenthesedFromItem) {
            ParenthesedFromItem pfi = (ParenthesedFromItem) fromItem;
            gatherFromItem(pfi.getFromItem(), aliases, ctx);
            if (pfi.getJoins() != null) {
                for (Join j : pfi.getJoins()) {
                    if (j.getRightItem() != null) gatherFromItem(j.getRightItem(), aliases, ctx);
                }
            }
        } else if (fromItem instanceof LateralSubSelect || fromItem instanceof TableFunction) {
            // not lineage-relevant
        }
    }

    private void registerVirtualAlias(String alias, ParenthesedSelect sub, ParseContext ctx) {
        // Map virtual alias columns to real source columns: alias.COL -> real table column
        List<String> virtualCols = new ArrayList<>();
        List<String> virtualExprs = new ArrayList<>();
        Select inner = sub.getSelect();
        if (inner instanceof PlainSelect) {
            for (SelectItem si : ((PlainSelect) inner).getSelectItems()) {
                virtualCols.add(si.getAlias() != null ? normalize(si.getAlias().getName()) : null);
                virtualExprs.add(si.getExpression().toString());
            }
        }
        ctx.aliasToTable.put(alias.toUpperCase() + ".VIRTUAL", alias);
        // store mapping alias.COL -> expression for field resolution
        ctx.virtualCols.put(alias.toUpperCase(), virtualCols);
        ctx.virtualExprs.put(alias.toUpperCase(), virtualExprs);
    }

    /**
     * Extract column mappings: for each select item, if it references columns of input tables,
     * produce mapping target column <- source columns.
     */
    private List<SqlLineageRes.FieldMapping> extractFieldMappings(Select select, List<String> targetColumns,
                                                                   Map<String, String> inputAliases, ParseContext ctx) {
        List<SqlLineageRes.FieldMapping> mappings = new ArrayList<>();
        List<Expression> selectItems = new ArrayList<>();
        List<String> itemAliases = new ArrayList<>();

        collectSelectItems(select, selectItems, itemAliases);

        for (int i = 0; i < selectItems.size(); i++) {
            Expression expr = selectItems.get(i);
            String targetCol = null;
            if (targetColumns != null && i < targetColumns.size()) {
                targetCol = targetColumns.get(i);
            } else if (itemAliases.get(i) != null) {
                targetCol = normalize(itemAliases.get(i));
            } else if (expr instanceof Column) {
                targetCol = normalize(((Column) expr).getColumnName());
            }
            if (targetCol == null) continue;

            if (expr instanceof Column) {
                Column col = (Column) expr;
                String tableName = resolveTableForColumn(col, inputAliases, ctx);
                if (tableName == null && col.getTable() == null && inputAliases.size() == 1) {
                    tableName = inputAliases.keySet().iterator().next();
                }
                String colName = normalize(col.getColumnName());
                if (tableName != null) {
                    SqlLineageRes.FieldMapping fm = new SqlLineageRes.FieldMapping();
                    fm.setSourceTable(tableName);
                    fm.setSourceColumn(colName);
                    fm.setTargetColumn(targetCol);
                    fm.setTransformation(null);
                    mappings.add(fm);
                }
            } else {
                Set<String> refs = new HashSet<>();
                collectColumnRefs(expr, refs);
                if (!refs.isEmpty()) {
                    String transformation = expr.toString();
                    for (String ref : refs) {
                        int dot = ref.indexOf('.');
                        String table = dot >= 0 ? ref.substring(0, dot) : null;
                        String col = dot >= 0 ? ref.substring(dot + 1) : ref;
                        String resolvedTable = null;
                        if (table != null) {
                            String t = ctx.aliasToTable.get(table.toUpperCase());
                            if (t != null) resolvedTable = t;
                        } else if (inputAliases.size() == 1) {
                            resolvedTable = inputAliases.keySet().iterator().next();
                        }
                        SqlLineageRes.FieldMapping fm = new SqlLineageRes.FieldMapping();
                        fm.setSourceTable(resolvedTable);
                        fm.setSourceColumn(normalize(col));
                        fm.setTargetColumn(targetCol);
                        fm.setTransformation(transformation);
                        mappings.add(fm);
                    }
                }
            }
        }
        return mappings;
    }

    private void collectSelectItems(Select select, List<Expression> items, List<String> aliases) {
        if (select instanceof PlainSelect) {
            for (SelectItem si : ((PlainSelect) select).getSelectItems()) {
                items.add(si.getExpression());
                aliases.add(si.getAlias() != null ? si.getAlias().getName() : null);
            }
        } else if (select instanceof SetOperationList) {
            for (Select s : ((SetOperationList) select).getSelects()) {
                collectSelectItems(s, items, aliases);
            }
        } else if (select instanceof ParenthesedSelect) {
            Select inner = ((ParenthesedSelect) select).getSelect();
            if (inner != null) collectSelectItems(inner, items, aliases);
        }
    }

    private String resolveTableForColumn(Column col, Map<String, String> inputAliases, ParseContext ctx) {
        if (col.getTable() != null && col.getTable().getName() != null) {
            String t = ctx.aliasToTable.get(col.getTable().getName().toUpperCase());
            if (t != null) return t;
            // maybe direct table name
            String direct = normalize(col.getTable().getName());
            if (inputAliases.containsKey(direct)) return direct;
        }
        return null;
    }

    private void collectColumnRefs(Expression expr, Set<String> refs) {
        if (expr instanceof Column) {
            Column c = (Column) expr;
            if (c.getTable() != null && c.getTable().getName() != null) {
                refs.add(c.getTable().getName() + "." + c.getColumnName());
            } else {
                refs.add(c.getColumnName());
            }
        } else if (expr instanceof Function) {
            Function f = (Function) expr;
            if (f.getParameters() != null) {
                for (Expression p : f.getParameters().getExpressions()) {
                    collectColumnRefs(p, refs);
                }
            }
        } else if (expr instanceof Parenthesis) {
            collectColumnRefs(((Parenthesis) expr).getExpression(), refs);
        } else if (expr instanceof net.sf.jsqlparser.expression.BinaryExpression) {
            collectColumnRefs(((net.sf.jsqlparser.expression.BinaryExpression) expr).getLeftExpression(), refs);
            collectColumnRefs(((net.sf.jsqlparser.expression.BinaryExpression) expr).getRightExpression(), refs);
        } else if (expr instanceof InExpression) {
            InExpression in = (InExpression) expr;
            if (in.getLeftExpression() != null) collectColumnRefs(in.getLeftExpression(), refs);
            if (in.getRightExpression() instanceof ExpressionList) {
                ExpressionList<?> el = (ExpressionList<?>) in.getRightExpression();
                for (Object o : el.getExpressions()) {
                    if (o instanceof Expression) collectColumnRefs((Expression) o, refs);
                }
            }
        } else if (expr instanceof CaseExpression) {
            CaseExpression ce = (CaseExpression) expr;
            if (ce.getSwitchExpression() != null) collectColumnRefs(ce.getSwitchExpression(), refs);
            if (ce.getWhenClauses() != null) {
                for (WhenClause wc : ce.getWhenClauses()) {
                    if (wc.getWhenExpression() != null) collectColumnRefs(wc.getWhenExpression(), refs);
                    if (wc.getThenExpression() != null) collectColumnRefs(wc.getThenExpression(), refs);
                }
            }
            if (ce.getElseExpression() != null) collectColumnRefs(ce.getElseExpression(), refs);
        } else if (expr instanceof CastExpression) {
            collectColumnRefs(((CastExpression) expr).getLeftExpression(), refs);
        }
    }

    private String normalize(String name) {
        if (name == null) return null;
        String n = name;
        int dot = n.indexOf('.');
        if (dot >= 0) n = n.substring(dot + 1);
        return n.replace("\"", "").toUpperCase();
    }
}