package com.temporedata.guard.modules.data.datasource.pool;

import com.temporedata.guard.modules.data.datasource.entity.DatasourceEntity;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages HikariCP connection pools per datasource.
 */
@Component
public class JdbcConnectionManager {

    private final int maxPoolSize;

    public JdbcConnectionManager(@Value("${zy.datasource-pool.max-size:8}") int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }

    private final Map<String, HikariDataSource> poolMap = new ConcurrentHashMap<>();

    public DataSource getOrCreate(DatasourceEntity ds, String plainPassword) {
        return poolMap.computeIfAbsent(ds.getId(), id -> createPool(ds, plainPassword));
    }

    public void remove(String datasourceId) {
        HikariDataSource ds = poolMap.remove(datasourceId);
        if (ds != null) {
            ds.close();
        }
    }

    public void refresh(DatasourceEntity ds, String plainPassword) {
        remove(ds.getId());
        poolMap.put(ds.getId(), createPool(ds, plainPassword));
    }

    private HikariDataSource createPool(DatasourceEntity ds, String plainPassword) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(buildJdbcUrl(ds));
        config.setUsername(ds.getUsername());
        config.setPassword(plainPassword);
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(0);
        config.setConnectionTimeout(10000);
        config.setIdleTimeout(60000);
        config.setMaxLifetime(300000);
        config.setPoolName("zy-ds-" + ds.getId());
        return new HikariDataSource(config);
    }

    private String buildJdbcUrl(DatasourceEntity ds) {
        switch (ds.getType().toUpperCase()) {
            case "MYSQL":
                return String.format("jdbc:mysql://%s:%d/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
                        ds.getHost(), ds.getPort(), ds.getDatabase());
            case "POSTGRESQL":
                return String.format("jdbc:postgresql://%s:%d/%s",
                        ds.getHost(), ds.getPort(), ds.getDatabase());
            case "H2":
                String h2Db = ds.getDatabase();
                if (h2Db != null && h2Db.trim().startsWith("jdbc:h2:")) {
                    return h2Db.trim();
                }
                if (h2Db != null && !h2Db.trim().isEmpty()) {
                    return "jdbc:h2:" + h2Db.trim() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
                }
                return "jdbc:h2:mem:temporedata_guard_query_" + ds.getId() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
            default:
                return "jdbc:h2:mem:temporedata_guard_query_" + ds.getId() + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        }
    }
}