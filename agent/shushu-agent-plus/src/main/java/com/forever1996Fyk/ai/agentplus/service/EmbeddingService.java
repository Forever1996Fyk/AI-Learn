package com.forever1996Fyk.ai.agentplus.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/21 15:35
 **/
@Slf4j
@Service
public class EmbeddingService {
    private static final String VECTOR_TABLE = "vector_file_info";
    private static final int EMBEDDING_BATCH_SIZE = 9;

    @Autowired(required = false)
    @Qualifier("pgVectorJdbcTemplate")
    private JdbcTemplate pgVectorJdbcTemplate;

    @Autowired
    private EmbeddingModel embeddingModel;
    @Autowired
    private OpenAiChatModel chatModel;

    private PgVectorStore vectorStore;

    @PostConstruct
    void init() {
        if (pgVectorJdbcTemplate == null) {
            log.warn("[dodo-agentx] PgVector 未配置，文件向量化/RAG 不可用，文件将走直接加载路径");
            return;
        }
        try {
            JdbcTemplateHelper helper = buildStore(pgVectorJdbcTemplate, embeddingModel);
            this.vectorStore = helper.store;
            log.info("[dodo-agentx] PgVectorStore 初始化完成: 表={}", VECTOR_TABLE);
        } catch (Exception e) {
            log.error("[dodo-agentx] PgVectorStore 初始化失败，RAG 不可用: {}", e.getMessage(), e);
        }
    }

    public boolean isAvailable() {
        return vectorStore != null;
    }

    /**
     * 切分 + 向量化 + 入库
     */
    public void embedAndStore(List<Document> documents, String fileId) {
        if (vectorStore == null) {
            log.warn("[dodo-agentx] PgVector 不可用，跳过向量化: fileId={}", fileId);
            return;
        }
        // 给每个 chunk 注入 fileid 元数据，供后续按文件过滤检索
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            doc.getMetadata().put("fileid", fileId);
            doc.getMetadata().put("chunkId", i);
        }
        for (int i = 0; i < documents.size(); i += EMBEDDING_BATCH_SIZE) {
            List<Document> batch = documents.subList(i, Math.min(i + EMBEDDING_BATCH_SIZE, documents.size()));
            vectorStore.doAdd(batch);
        }
        log.info("[dodo-agentx] 向量化完成: fileId={}, chunks={}", fileId, documents.size());
    }


    /**
     * 构建 PgVectorStore 的内部 helper
     */
    private JdbcTemplateHelper buildStore(JdbcTemplate jdbc, EmbeddingModel embeddingModel) {
        PgVectorStore store = PgVectorStore.builder(jdbc, embeddingModel)
                .dimensions(1024)
                .distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
                .indexType(PgVectorStore.PgIndexType.HNSW)
                .initializeSchema(true)
                .removeExistingVectorStoreTable(false)
                .vectorTableName(VECTOR_TABLE)
                .maxDocumentBatchSize(100)
                .build();
        try {
            store.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new JdbcTemplateHelper(store);
    }

    private record JdbcTemplateHelper(PgVectorStore store) {
    }
}
