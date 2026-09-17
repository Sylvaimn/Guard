package com.temporedata.guard.modules.data.datasource.plugin;

import com.temporedata.guard.api.base.exceptions.BusinessException;
import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.temporedata.guard.modules.data.datasource.pool.JdbcConnectionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * JDBC 插件: MySQL / H2 / PostgreSQL 等标准 JDBC 数据源(内置默认)。
 * 复用现有连接池, 查询/执行按 JDBC 通用实现。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JdbcDataSourcePlugin implements DataSourcePlugin {

    private final JdbcConnectionManager connectionManager;

    @org.springframework.beans.factory.annotation.Value("${zy.crypto.key:}")
    private String cryptoKey;

    @Override
    public String type() {
        return "JDBC";
    }

    @Override
    public java.util.List<String> aliases() {
        return java.util.List.of("MYSQL", "H2", "POSTGRESQL", "POSTGRES");
    }

    @Override
    public String[] capabilities() {
        return new String[]{"query", "execute", "test", "meta"};
    }

    @Override
    public boolean test(DatasourceEntity ds) {
        try (Connection conn = acquire(ds)) {
            return conn.isValid(5);
        } catch (Exception e) {
            log.warn("JDBC test failed: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> query(DatasourceEntity ds, String sql) {
        List<Map<String, Object>> rows = new ArrayList<>();
        try (Connection conn = acquire(ds);
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= cols; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
            }
        } catch (Exception e) {
            throw new BusinessException("JDBC 查询失败: " + e.getMessage());
        }
        return rows;
    }

    @Override
    public int execute(DatasourceEntity ds, String sql) {
        try (Connection conn = acquire(ds);
             Statement st = conn.createStatement()) {
            return st.executeUpdate(sql);
        } catch (Exception e) {
            throw new BusinessException("JDBC 执行失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> meta(DatasourceEntity ds) {
        List<Map<String, Object>> tables = new ArrayList<>();
        try (Connection conn = acquire(ds)) {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> t = new LinkedHashMap<>();
                    t.put("name", rs.getString("TABLE_NAME"));
                    t.put("type", "TABLE");
                    tables.add(t);
                }
            }
        } catch (Exception e) {
            log.warn("JDBC meta failed: {}", e.getMessage());
        }
        return tables;
    }

    private Connection acquire(DatasourceEntity ds) {
        String plain = com.temporedata.guard.common.util.CryptoUtil.decrypt(ds.getPassword(), cryptoKey);
        javax.sql.DataSource dataSource = connectionManager.getOrCreate(ds, plain);
        try {
            return dataSource.getConnection();
        } catch (Exception e) {
            throw new BusinessException("获取连接失败: " + e.getMessage());
        }
    }
}
