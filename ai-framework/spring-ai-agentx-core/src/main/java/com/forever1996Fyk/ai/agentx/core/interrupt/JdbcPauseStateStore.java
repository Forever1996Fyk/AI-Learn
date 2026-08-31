package com.forever1996Fyk.ai.agentx.core.interrupt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/31 17:15
 **/
public class JdbcPauseStateStore implements PauseStateStore {


    private static final Logger log = LoggerFactory.getLogger(JdbcPauseStateStore.class);

    static final String CREATE_TABLE_SQL = """
            CREATE TABLE IF NOT EXISTS agentx_pause_state (
                id                BIGINT       NOT NULL AUTO_INCREMENT  COMMENT '主键',
                conversation_id   VARCHAR(100) NOT NULL                 COMMENT '会话ID',
                reason            VARCHAR(50)  NOT NULL                 COMMENT '暂停原因',
                safe_point        VARCHAR(50)  DEFAULT NULL             COMMENT '中断安全点',
                current_round     INT          NOT NULL                 COMMENT '中断时已完成的轮次',
                interrupted_at    TIMESTAMP    NOT NULL                 COMMENT '中断时间',
                expires_at        TIMESTAMP    DEFAULT NULL             COMMENT '过期时间',
                snapshot_json     LONGTEXT     NOT NULL                 COMMENT '完整 PauseState JSON',
                PRIMARY KEY (id),
                UNIQUE KEY uk_conversation_id (conversation_id),
                INDEX idx_pause_state_expires (expires_at)
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AgentX 暂停现场快照表'
            """;

    /**
     * 默认 TTL：7 天（毫秒）
     */
    static final long DEFAULT_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000;

    private final JdbcTemplate jdbcTemplate;
    private final long ttlMillis;
    private volatile boolean initialized;

    public JdbcPauseStateStore(DataSource dataSource) {
        this(dataSource, DEFAULT_TTL_MILLIS);
    }

    public JdbcPauseStateStore(DataSource dataSource, long ttlMillis) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.ttlMillis = ttlMillis;
    }

    /**
     * 自动建表（幂等），工厂方法在构造后调用。
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            jdbcTemplate.execute(CREATE_TABLE_SQL);
            initialized = true;
            log.info("[JdbcPauseStateStore] table agentx_pause_state ready");
        }
    }

}
