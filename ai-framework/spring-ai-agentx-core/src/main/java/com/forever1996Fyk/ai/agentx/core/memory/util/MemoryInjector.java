package com.forever1996Fyk.ai.agentx.core.memory.util;

import com.forever1996Fyk.ai.agentx.core.memory.LongTermMemoryManager;
import com.forever1996Fyk.ai.agentx.core.model.RunnableParams;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 *
 * 长期记忆注入器 — 在 Agent 开始执行前按 userId + query 语义检索相关记忆，
 * 格式化为 system prompt 区块。
 *
 * @author: YuKai Fan
 * @create: 2026/9/1 09:35
 **/
public class MemoryInjector {

    private static final Logger log = LoggerFactory.getLogger(MemoryInjector.class);

    private final LongTermMemoryManager longTermMemoryManager;

    public MemoryInjector(LongTermMemoryManager longTermMemoryManager) {
        this.longTermMemoryManager = longTermMemoryManager;
    }

    public String buildMemorySection(RunnableParams params, String query) {
        if (longTermMemoryManager == null || params == null || StringUtils.isBlank(params.getUserId())) {
            return "";
        }
        if (StringUtils.isBlank(query)) {
            return "";
        }
        try {
            List<Document> docs = longTermMemoryManager.searchRelevant(params.getUserId(), query);
            return LongTermMemoryPromptFormatter.formatSection(docs);
        } catch (Exception e) {
            log.error("Long-term memory search failed for userId={}: {}",
                    params.getUserId(), e.getMessage());
            return "";
        }
    }
}
