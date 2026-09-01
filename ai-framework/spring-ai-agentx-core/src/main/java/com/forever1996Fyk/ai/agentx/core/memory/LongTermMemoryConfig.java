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
    private final int topK;
    private final double similarityThreshold;

    public LongTermMemoryConfig(VectorStore vectorStore, int topK, double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.topK = topK;
        this.similarityThreshold = similarityThreshold;
    }

    public VectorStore getVectorStore() {
        return vectorStore;
    }

    public int getTopK() {
        return topK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }
}
