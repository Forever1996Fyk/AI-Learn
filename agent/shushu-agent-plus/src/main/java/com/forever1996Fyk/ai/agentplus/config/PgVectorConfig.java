package com.forever1996Fyk.ai.agentplus.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PgVector 专用 JdbcTemplate（独立于主 MySQL 数据源）。
 * <p>
 * 只暴露 JdbcTemplate 而非 DataSource：避免污染 DataSource 类型候选，
 * 让 Spring Boot 默认的 DataSourceAutoConfiguration 正常接管主库 MySQL。
 */
@Configuration
@ConditionalOnProperty(name = "embeddings.store.host")
public class PgVectorConfig {

    @Bean(name = "pgVectorJdbcTemplate")
    public JdbcTemplate pgVectorJdbcTemplate(
            @Value("${embeddings.store.host}") String host,
            @Value("${embeddings.store.port}") String port,
            @Value("${embeddings.store.database}") String database,
            @Value("${embeddings.store.user}") String user,
            @Value("${embeddings.store.password}") String password) {

        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl("jdbc:postgresql://" + host + ":" + port + "/" + database);
        ds.setUsername(user);
        ds.setPassword(password);
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(20);
        ds.setMinimumIdle(2);
        ds.setPoolName("dodo-agentx-PgVectorPool");
        return new JdbcTemplate(ds);
    }
}
