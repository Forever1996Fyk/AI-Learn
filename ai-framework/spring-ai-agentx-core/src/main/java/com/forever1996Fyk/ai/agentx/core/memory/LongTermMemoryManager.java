package com.forever1996Fyk.ai.agentx.core.memory;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

/**
 * @program: AI-Learn
 * @description: 长期记忆管理器

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
 *
 * @author: YuKai Fan
 * @create: 2026/8/31 15:56
 **/
public class LongTermMemoryManager {

    private final VectorStore vectorStore;
    private final ChatModel chatModel;
    private final LongTermMemoryConfig config;


    public LongTermMemoryManager(LongTermMemoryConfig config, ChatModel chatModel) {
        this.chatModel = chatModel;
        this.config = config;
        this.vectorStore = config.getVectorStore();
    }
}
