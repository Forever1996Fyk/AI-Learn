package com.forever1996Fyk.ai.intelligent.customer.rag.controller;

import com.forever1996Fyk.ai.intelligent.customer.ai.service.CommonChatService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.rag.config.ElasticSearchConfiguration;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.reranker.BgeScoringModel;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.IntelligentCustomerElasticsearchContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.router.IntelligentCustomerQueryRouter;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.transformer.IntelligentCustomerQueryTransformer;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.ContentMetadata;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.retriever.elasticsearch.ElasticsearchContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.router.DefaultQueryRouter;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationFullText;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import jakarta.servlet.http.HttpServletResponse;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description: 用于ai的各个模块的功能测试
 * @author: YuKai Fan
 * @create: 2026/6/6 22:40
 **/
@RestController
@RequestMapping("/ai/module")
public class RagModuleController {
    @Autowired
    private ChatModel chatModel;

    @Autowired
    private StreamingChatModel streamingChatModel;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Autowired
    private RestClient restClient;

    private static final int MAX_RESULT = 5;

    private static final double MIN_SCORE = 0.5;

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @GetMapping("testRetriever")
    public Flux<String> testRetriever(String query, String chatMessageId, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        ElasticsearchContentRetriever fullTextRetriever = ElasticsearchContentRetriever.builder()
                .restClient(restClient)
                .configuration(ElasticsearchConfigurationFullText.builder().build())
                .maxResults(MAX_RESULT)
                .indexName(ElasticSearchConfiguration.INDEX_NAME)
                .minScore(MIN_SCORE)
                .build();

        ElasticsearchContentRetriever embeddingRetriever = ElasticsearchContentRetriever
                .builder()
                .restClient(restClient)
                .embeddingModel(openAiEmbeddingModel)
                .configuration(ElasticsearchConfigurationKnn.builder().build())
                .maxResults(MAX_RESULT)
                .indexName(ElasticSearchConfiguration.INDEX_NAME)
                .minScore(MIN_SCORE)
                .build();

        IntelligentCustomerQueryTransformer queryTransformer = new IntelligentCustomerQueryTransformer(chatModel, chatMessageId);
        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new DefaultQueryRouter(embeddingRetriever, fullTextRetriever))
                .queryTransformer(queryTransformer)
                .build();
        CommonChatService service = AiServices.builder(CommonChatService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
        return service.streamChat(query);
    }

    @GetMapping("testRetriever2")
    public Flux<String> testRetriever2(String query, String chatMessageId, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        ElasticsearchContentRetriever fullTextRetriever = ElasticsearchContentRetriever.builder()
                .restClient(restClient)
                .configuration(ElasticsearchConfigurationFullText.builder().build())
                .maxResults(MAX_RESULT)
                .indexName(ElasticSearchConfiguration.INDEX_NAME)
                .minScore(MIN_SCORE)
                .build();

        IntelligentCustomerElasticsearchContentRetriever embeddingRetriever = IntelligentCustomerElasticsearchContentRetriever
                .builder()
                .restClient(restClient)
                .embeddingModel(openAiEmbeddingModel)
                .configuration(ElasticsearchConfigurationKnn.builder().build())
                .maxResults(MAX_RESULT)
                .indexName(ElasticSearchConfiguration.INDEX_NAME)
                .minScore(MIN_SCORE)
                .knowledgeSegmentService(knowledgeSegmentService)
                .build();

        IntelligentCustomerQueryTransformer queryTransformer = new IntelligentCustomerQueryTransformer(chatModel, chatMessageId);
        DefaultRetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(new DefaultQueryRouter(embeddingRetriever, fullTextRetriever))
                .queryTransformer(queryTransformer)
                .build();
        CommonChatService service = AiServices.builder(CommonChatService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .build();
        return service.streamChat(query);
    }

    @GetMapping("testReranker")
    public String testReranker(String query) {
        if (!StringUtils.hasText(query)) {
            query = "什么是Java?";
        }
        // 1. 获取BGE-RERANKER
        OnnxScoringModel scoringModel = BgeScoringModel.getInstance();
        // 2. 构造测试文档，模拟检索结果
        List<Content> testContents = List.of(
                Content.from(TextSegment.from("Java是一种面向对象的编程语言，具有跨平台、安全性高等特点，广泛应用于企业级开发。")),
                Content.from(TextSegment.from("Python是一种解释型的高级编程语言，以简洁易读的语法著称，常用于数据科学和人工智能领域。")),
                Content.from(TextSegment.from("JavaScript是一种脚本语言，主要用于Web前端开发，也可以通过Node.js进行服务端编程。")),
                Content.from(TextSegment.from("Java虚拟机（JVM）是运行Java字节码的虚拟机，它使得Java具有跨平台能力。Spring是最流行的Java开发框架。")),
                Content.from(TextSegment.from("Go语言由Google开发，以高并发和简洁语法为特色，常用于微服务和云原生开发。"))
        );

        // 3. 构建 ReRankingContentAggregator
        ContentAggregator aggregator = ReRankingContentAggregator.builder()
                .scoringModel(scoringModel)
                .build();

        // 4. 直接调用 ContentAggregator 进行重排序
        Query queryObj = new Query(query);
        List<Content> rerankedContents = aggregator.aggregate(Map.of(queryObj, List.of(testContents)));
        // 5. 格式化输出结果
        return rerankedContents.stream()
                .map(content -> {
                    TextSegment segment = content.textSegment();
                    Double rerankedScore = (Double) content.metadata().get(ContentMetadata.RERANKED_SCORE);
                    return String.format("[rerankedScore=%.4f] %s",
                            rerankedScore != null ? rerankedScore : 0.0,
                            segment.text());
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
