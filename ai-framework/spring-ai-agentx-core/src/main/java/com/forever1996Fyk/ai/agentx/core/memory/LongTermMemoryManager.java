package com.forever1996Fyk.ai.agentx.core.memory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;

import java.util.List;

/**
 * @program: AI-Learn
 * @description: 长期记忆管理器
 *
 * <p>封装跨会话记忆的「抽取 - 去重 - 合并 - 检索」全流程，所有数据落在
 * 单一 doc type {@value #DOC_TYPE} 的 PgVectorStore 表中。
 *
 * <h3>写入路径</h3>
 * <ol>
 *   <li>LLM 调用 #1：从本次调用 transcript 抽取候选记忆（JSON 数组）</li>
 *   <li>对每条候选：embedding 检索 top-K 相似记忆</li>
 *   <li>命中 → LLM 调用 #2 合并旧 + 新 → delete 旧 + insert 合并</li>
 *   <li>未命中 → 直接 insert</li>
 * </ol>
 *
 * <h3>读取路径</h3>
 * 按 userId + query 语义检索 top-K 相关记忆，注入 SystemMessage。
 * @author: YuKai Fan
 * @create: 2026/8/31 15:56
 **/
public class LongTermMemoryManager {
    private static final Logger log = LoggerFactory.getLogger(LongTermMemoryManager.class);

    public static final String DOC_TYPE = "memory";
    public static final String META_TYPE = "type";
    public static final String META_USER_ID = "user_id";

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final LongTermMemoryConfig config;


    public LongTermMemoryManager(LongTermMemoryConfig config, ChatModel chatModel) {
        this.chatModel = chatModel;
        this.config = config;
        this.vectorStore = config.getVectorStore();
    }

    // ==================== 读取：检索注入 ====================

    /**
     * 按用户和当前 query 检索 top-K 相关记忆，供 SystemMessage 注入。
     *
     * @param userId 用户ID
     * @param query  查询内容
     * @return 相关记忆列表
     */
    public List<Document> searchRelevant(String userId, String query) {
        if (StringUtils.isAnyBlank(userId, query)) {
            return List.of();
        }
        try {
            return vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(query)
                            .topK(config.getTopK())
                            .similarityThreshold(config.getSimilarityThreshold())
                            .filterExpression(userMemoryFilter(userId))
                            .build()
            );
        } catch (Exception e) {
            log.warn("Memory search failed: userId={}, err={}", userId, e.getMessage());
            return List.of();
        }
    }

    /**
     * 构建向量过滤器（元数据 metadata 过滤）
     * @param userId
     * @return
     */
    private Filter.Expression userMemoryFilter(String userId) {
        FilterExpressionBuilder filterExpressionBuilder = new FilterExpressionBuilder();
        return filterExpressionBuilder.and(
                filterExpressionBuilder.eq(META_USER_ID, userId),
                filterExpressionBuilder.eq(META_TYPE, DOC_TYPE)
        ).build();
    }
}
