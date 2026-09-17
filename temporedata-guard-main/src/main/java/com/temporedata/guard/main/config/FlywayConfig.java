package com.temporedata.guard.main.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Flyway database migration configuration.
 * Enables version-controlled database schema management.
 * Automatically disabled for H2 dev profile (JPA ddl-auto handles it).
 */
@Configuration
@ConditionalOnProperty(name = "spring.flyway.enabled", havingValue = "true", matchIfMissing = false)
public class FlywayConfig {

    @Value("${spring.flyway.locations:classpath:db/migration}")
    private String migrationLocation;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .locations(migrationLocation)
                .baselineOnMigrate(true)
                .load();
    }
}