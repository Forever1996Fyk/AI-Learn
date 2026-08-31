package com.forever1996Fyk.ai.agentx.core.memory;

import org.springframework.ai.vectorstore.VectorStore;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/31 16:14
 **/
public class LongTermMemoryConfig {

    private final VectorStore vectorStore;

    public LongTermMemoryConfig(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }
}
