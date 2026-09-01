package com.forever1996Fyk.ai.agentx.core.memory.store;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 *
 * 会话消息链存储 — agentx_session 表的 CRUD。
 * 每条消息一行，按 conversation_id + state_key 聚合，item_index 保留顺序。
 * 终态时批量追加本次调用新增的消息，ReAct 循环中不触碰 DB。
 *
 * @author: YuKai Fan
 * @create: 2026/8/31 15:30
 **/
public class SessionMessageStore {

    private static final Logger log = LoggerFactory.getLogger(SessionMessageStore.class);

    private static final String CREATE_TABLE_SQL = """
            CREATE TABLE agentx_session (
                id              BIGINT       NOT NULL  COMMENT '主键ID',
                conversation_id VARCHAR(100) NOT NULL  COMMENT '会话窗口ID',
                session_id      VARCHAR(100) NOT NULL  COMMENT '本次调用ID',
                state_key       VARCHAR(255) NOT NULL  COMMENT '状态键: original_messages / working_messages / offload_context',
                item_index      INT          NOT NULL DEFAULT 0 COMMENT '消息在状态键内的序号',
                state_data      LONGTEXT     NOT NULL  COMMENT '消息JSON（MessageJsonSerializer 序列化）',
                created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (id)
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AgentX会话消息链表'
            """;

    private static final String CREATE_IDX_SESSION_SQL = """
            CREATE INDEX idx_session_state ON agentx_session (session_id, state_key)
            """;

    private static final String CREATE_IDX_CONV_SQL = """
            CREATE INDEX idx_conv_state ON agentx_session (conversation_id, state_key)
            """;

    private final JdbcTemplate jdbcTemplate;
    private volatile boolean initialized = false;

    public SessionMessageStore(DataSource dataSource) {
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
                        jdbcTemplate.execute(CREATE_IDX_SESSION_SQL);
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

    /**
     * 加载某会话窗口指定状态键的全部消息，按 item_index 顺序合并。
     * 用于多轮对话加载历史上下文。
     */
    public List<Message> getMessages(String conversationId, String stateKey) {
        return null;
    }
}
