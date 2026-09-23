package com.forever1996Fyk.ai.agentplus.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.ai.rag.preretrieval.query.transformation.CompressionQueryTransformer;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
            log.warn("[shushu-agent-plus] PgVector 未配置，文件向量化/RAG 不可用，文件将走直接加载路径");
            return;
        }
        try {
            JdbcTemplateHelper helper = buildStore(pgVectorJdbcTemplate, embeddingModel);
            this.vectorStore = helper.store;
            log.info("[shushu-agent-plus] PgVectorStore 初始化完成: 表={}", VECTOR_TABLE);
        } catch (Exception e) {
            log.error("[shushu-agent-plus] PgVectorStore 初始化失败，RAG 不可用: {}", e.getMessage(), e);
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
            log.warn("[shushu-agent-plus] PgVector 不可用，跳过向量化: fileId={}", fileId);
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
        log.info("[shushu-agent-plus] 向量化完成: fileId={}, chunks={}", fileId, documents.size());
    }


    /**
     * RAG 检索：问题压缩 → 问题扩展 → 向量召回 → topK=5 → 按 fileid 过滤去重
     */
    public List<String> ragRetrieve(String fileId, String question) {
        if (vectorStore == null) {
            return Collections.emptyList();
        }
        if (StringUtils.isAnyBlank(fileId, question)) {
            return Collections.emptyList();
        }
        try {
            Query query = Query.builder().text(question).build();
            ChatClient chatClient = ChatClient.builder(chatModel).build();
            CompressionQueryTransformer compressor = CompressionQueryTransformer.builder()
                    .chatClientBuilder(chatClient.mutate())
                    .build();
            Query compressed = compressor.transform(query);
            log.info("[shushu-agent-plus] RAG 问题压缩: {}", compressed.text());

            MultiQueryExpander expander = MultiQueryExpander.builder()
                    .chatClientBuilder(chatClient.mutate())
                    .numberOfQueries(3)
                    .includeOriginal(true)
                    .build();
            List<Query> expandedQueries = expander.expand(compressed);
            Filter.Expression filter = new FilterExpressionBuilder()
                    .eq("fileid", fileId)
                    .build();
            List<String> results = new ArrayList<>();
            Set<String> seenIds = new HashSet<>();
            for (Query expandedQuery : expandedQueries) {
                List<Document> docs = vectorStore.similaritySearch(
                        SearchRequest.builder()
                                .query(expandedQuery.text())
                                .topK(5)
                                .filterExpression(filter)
                                .build()
                );
                for (Document doc : docs) {
                    if (seenIds.add(doc.getId())) {
                        results.add(doc.getText());
                    }
                }
            }
            log.info("[shushu-agent-plus] RAG 检索完成: fileId={}, 结果数={}", fileId, results.size());
            return results;
        } catch (Exception e) {
            log.error("[shushu-agent-plus] RAG 检索失败: fileId={}", fileId, e);
            return Collections.emptyList();
        }
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
