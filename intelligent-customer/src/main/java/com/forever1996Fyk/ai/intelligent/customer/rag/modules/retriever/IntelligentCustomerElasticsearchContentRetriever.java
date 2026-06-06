package com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever;

import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.elasticsearch.ElasticsearchContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.elasticsearch.AbstractElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfiguration;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationFullText;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationHybrid;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.poi.ss.formula.functions.T;
import org.elasticsearch.client.RestClient;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/6 23:08
 **/
@Slf4j
public class IntelligentCustomerElasticsearchContentRetriever extends AbstractElasticsearchEmbeddingStore implements ContentRetriever {
    private final EmbeddingModel embeddingModel;
    private final int maxResults;
    private final double minScore;
    private final Filter filter;

    private final KnowledgeSegmentService knowledgeSegmentService;
    /**
     * Creates an instance of ElasticsearchContentRetriever using a RestClient.
     *
     * @param configuration  Elasticsearch retriever configuration to use (knn, script, full text, hybrid, hybrid with reranker)
     * @param restClient     Elasticsearch Rest Client (mandatory)
     * @param indexName      Elasticsearch index name (optional). Default value: "default".
     *                       Index will be created automatically if not exists.
     * @param embeddingModel Embedding model to be used by the retriever
     * @param maxResults     Maximum number of results to retrieve
     * @param minScore       Minimum score threshold for retrieved results
     * @param filter         Filter to apply during retrieval
     */
    public IntelligentCustomerElasticsearchContentRetriever(
            ElasticsearchConfiguration configuration,
            RestClient restClient,
            String indexName,
            EmbeddingModel embeddingModel,
            final int maxResults,
            final double minScore,
            final Filter filter,
            KnowledgeSegmentService knowledgeSegmentService) {
        this.embeddingModel = embeddingModel;
        this.maxResults = maxResults;
        this.minScore = minScore;
        this.filter = filter;
        this.knowledgeSegmentService = knowledgeSegmentService;
        this.initialize(configuration, restClient, indexName);
    }

    /**
     * 根据查询条件检索相关内容
     * 整体参考了 {@link ElasticsearchContentRetriever}, 这里的自定义ContentRetriever 主要是为了实现父子分段和兄弟分段查询
     * <p>
     * 根据当前配置的检索模式执行内容检索，支持以下三种模式：
     * <ul>
     *   <li><b>全文检索</b>：当配置为 {@link ElasticsearchConfigurationFullText} 时，直接执行全文搜索</li>
     *   <li><b>混合检索</b>：当配置为 {@link ElasticsearchConfigurationHybrid} 时，结合向量检索和全文检索</li>
     *   <li><b>向量检索（默认）</b>：将查询文本向量化后进行 KNN 相似度搜索</li>
     * </ul>
     * <p>
     * 在向量检索模式下，检索结果还会进行关联内容扩展：
     * <ul>
     *   <li>兄弟分段扩展：根据 brotherChunkId 检索同一父分段下的其他兄弟分段，补全上下文</li>
     *   <li>父分段替换：根据 parentChunkId 从 Redis 中读取父分段的完整文本，替换子分段以获得更完整的语义</li>
     * </ul>
     *
     * @param query 查询对象，包含待检索的文本内容
     * @return 检索到的内容列表，包含原始检索结果及扩展的关联内容
     */
    @Override
    public List<Content> retrieve(Query query) {
        // 将查询文本转换为向量
        Embedding referenceEmbedding = embeddingModel.embed(query.text()).content();
        EmbeddingSearchRequest request = EmbeddingSearchRequest.builder()
                .queryEmbedding(referenceEmbedding)
                .maxResults(maxResults)
                .minScore(minScore)
                .filter(filter)
                .build();
        List<Content> searchContents;

        if (configuration instanceof ElasticsearchConfigurationFullText) {
            // 全文检索模式：直接执行全文检索并返回结果
            log.debug("Using a full text search query");

            searchContents = this.fullTextSearch(query.text()).stream()
                    .map(t -> Content.from(
                            t,
                            Map.of(
                                    ContentMetadata.SCORE, Objects.requireNonNull(t.metadata().getDouble(ContentMetadata.SCORE.name())),
                                    ContentMetadata.EMBEDDING_ID, Objects.requireNonNull(t.metadata().getString(ContentMetadata.EMBEDDING_ID.name()))
                            )
                    )).toList();
        } else if (configuration instanceof ElasticsearchConfigurationHybrid) {
            // 混合检索模式：结合向量检索和全文检索 （只有企业版ES支持）
            searchContents = mapResultsToContentList(this.hybridSearch(request, query.text()));
        } else {
            searchContents = mapResultsToContentList(this.search(request));
        }

        // 去重并按文本内容排序
        searchContents = searchContents.stream().distinct().sorted(Comparator.comparing(content -> content.textSegment().text())).toList();

        List<Content> finalContents = Lists.newArrayList(searchContents);
        // 兄弟分段缓存与父分段缓存，避免重复查询
        Map<String, List<Content>> brotherDocMap = Maps.newHashMap();
        Map<String, List<Content>> parentDocMap = Maps.newHashMap();

        for (Content content : searchContents) {
            // 兄弟分段扩展：检索具有相同 brotherChunkId 的其他兄弟分段
            String brotherChunkId = content.textSegment().metadata().getString(MetadataKeyConstant.BROTHER_CHUNK_ID);
            if (StringUtils.hasText(brotherChunkId)) {
                List<Content> cachedBrotherDocs = brotherDocMap.get(brotherChunkId);
                if (CollectionUtils.isNotEmpty(cachedBrotherDocs)) {
                    // 命中缓存，直接使用已检索的兄弟分段
                    finalContents.addAll(cachedBrotherDocs);
                } else {
                    // 未命中缓存。按brotherChunkId过滤兄弟分段
                    Filter brotherFilter = MetadataFilterBuilder.metadataKey(MetadataKeyConstant.BROTHER_CHUNK_ID).isEqualTo(brotherChunkId);
                    request = EmbeddingSearchRequest.builder()
                            .filter(brotherFilter)
                            .build();
                    List<Content> brotherDocs = mapResultsToContentList(this.search(request));
                    brotherDocMap.put(brotherChunkId, brotherDocs);
                    finalContents.addAll(brotherDocs);
                }
            }

            // 父分段替换：用父分段的完整文本替换子分段，获取更完整的语义
            String parentChunkId = content.textSegment().metadata().getString(MetadataKeyConstant.PARENT_CHUNK_ID);
            if (StringUtils.hasText(parentChunkId)) {
                List<Content> cacheParentDocs = parentDocMap.get(parentChunkId);
                if (CollectionUtils.isNotEmpty(cacheParentDocs)) {
                    // 如果已经缓存中有过这个父分段了，说明已经用过了，这里就不用再加了，避免重复
                    finalContents.remove(content);
                } else if (knowledgeSegmentService != null) {
                    // 读取parentChunk的文本内容
                    String segmentText = knowledgeSegmentService.getTextByChunkId(parentChunkId);
                    if (StringUtils.hasText(segmentText)) {
                        // 用付分段文本构造新的Content，替换当前的子分段内容
                        TextSegment parentSegment = TextSegment.from(segmentText);
                        Content parentContent = Content.from(parentSegment, content.metadata());
                        List<Content> parentDocs = List.of(parentContent);
                        parentDocMap.put(parentChunkId, parentDocs);
                        finalContents.remove(content);
                        finalContents.addAll(parentDocs);
                    } else {
                        log.warn("parentChunk not found, chunkId:{}", parentChunkId);
                        finalContents.remove(content);
                    }
                }
            }
        }
        return searchContents;
    }

    private List<Content> mapResultsToContentList(EmbeddingSearchResult<TextSegment> searchResult) {
        List<Content> result = searchResult.matches().stream()
                .filter(f -> f.score() > minScore)
                .map(m -> Content.from(
                        m.embedded(),
                        Map.of(
                                ContentMetadata.SCORE, m.score(),
                                ContentMetadata.EMBEDDING_ID, m.embeddingId()
                        )
                )).toList();
        log.debug("Found [{}] relevant documents in Elasticsearch index [{}].", result.size(), indexName);
        return result;
    }


    public static IntelligentCustomerElasticsearchContentRetriever.Builder builder() {
        return new IntelligentCustomerElasticsearchContentRetriever.Builder();
    }

    public static class Builder {

        private RestClient restClient;
        private String indexName = "default";
        private ElasticsearchConfiguration configuration =
                ElasticsearchConfigurationKnn.builder().build();
        private EmbeddingModel embeddingModel;
        private int maxResults;
        private double minScore;
        private Filter filter;
        private KnowledgeSegmentService knowledgeSegmentService;

        /**
         * @param restClient Elasticsearch RestClient.
         * @return builder
         */
        public IntelligentCustomerElasticsearchContentRetriever.Builder restClient(RestClient restClient) {
            this.restClient = restClient;
            return this;
        }

        /**
         * @param indexName Elasticsearch index name (optional). Default value: "default".
         * @return builder
         */
        public IntelligentCustomerElasticsearchContentRetriever.Builder indexName(String indexName) {
            this.indexName = indexName;
            return this;
        }

        /**
         * @param configuration the configuration to use
         * @return builder
         */
        public IntelligentCustomerElasticsearchContentRetriever.Builder configuration(ElasticsearchConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public IntelligentCustomerElasticsearchContentRetriever.Builder embeddingModel(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }

        public IntelligentCustomerElasticsearchContentRetriever.Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public IntelligentCustomerElasticsearchContentRetriever.Builder minScore(double minScore) {
            this.minScore = minScore;
            return this;
        }

        public IntelligentCustomerElasticsearchContentRetriever.Builder filter(Filter filter) {
            this.filter = filter;
            return this;
        }


        public IntelligentCustomerElasticsearchContentRetriever.Builder knowledgeSegmentService(KnowledgeSegmentService knowledgeSegmentService) {
            this.knowledgeSegmentService = knowledgeSegmentService;
            return this;
        }

        public IntelligentCustomerElasticsearchContentRetriever build() {
            return new IntelligentCustomerElasticsearchContentRetriever(
                    configuration, restClient, indexName, embeddingModel, maxResults, minScore, filter, knowledgeSegmentService);
        }
    }
}
