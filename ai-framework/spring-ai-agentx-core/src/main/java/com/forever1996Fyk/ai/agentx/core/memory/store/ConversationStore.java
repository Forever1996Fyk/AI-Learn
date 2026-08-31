package com.forever1996Fyk.ai.agentx.core.memory.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * @program: AI-Learn
 * @description:
 *
 * 会话窗口存储 — agentx_conversation 表的 CRUD。
 * 每次调用一行，记录 question 与执行状态，供前端展示和历史回放。
 *
 * @author: YuKai Fan
 * @create: 2026/8/31 15:41
 **/
public class ConversationStore {

    private static final Logger log = LoggerFactory.getLogger(ConversationStore.class);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE agentx_conversation (
                id              BIGINT       NOT NULL  COMMENT '主键ID',
                conversation_id VARCHAR(100) NOT NULL  COMMENT '会话窗口ID',
                session_id      VARCHAR(100) NOT NULL  COMMENT '本次调用ID',
                user_id         VARCHAR(100) DEFAULT NULL COMMENT '用户ID',
                question        LONGTEXT     NOT NULL  COMMENT '用户提问',
                status          VARCHAR(20)  DEFAULT 'running' COMMENT '执行状态: running/completed/interrupted/error',
                created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                completed_at    TIMESTAMP    DEFAULT NULL COMMENT '完成时间',
                PRIMARY KEY (id)
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AgentX会话窗口表'
            """;

    private static final String CREATE_UK_SESSION_SQL = """
            CREATE UNIQUE INDEX uk_conv_session ON agentx_conversation (session_id)
            """;

    private static final String CREATE_IDX_CONV_SQL = """
            CREATE INDEX idx_conv_id ON agentx_conversation (conversation_id)
            """;

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean initialized = false;

    public ConversationStore(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public void initialize() {
        ensureInitialized();
    }

    private void ensureInitialized() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    try {
                        jdbcTemplate.execute(CREATE_TABLE_SQL);
                    } catch (Exception e) {
                        log.debug("Table agentx_conversation creation skipped (may already exist): {}", e.getMessage());
                    }
                    try {
                        jdbcTemplate.execute(CREATE_UK_SESSION_SQL);
                    } catch (Exception e) {
                        log.debug("Unique index uk_conv_session creation skipped (may already exist): {}", e.getMessage());
                    }
                    try {
                        jdbcTemplate.execute(CREATE_IDX_CONV_SQL);
                    } catch (Exception e) {
                        log.debug("Index idx_conv_id skipped: {}", e.getMessage());
                    }
                    initialized = true;
                    log.info("agentx_conversation table initialized");
                }
            }
        }
    }
}
