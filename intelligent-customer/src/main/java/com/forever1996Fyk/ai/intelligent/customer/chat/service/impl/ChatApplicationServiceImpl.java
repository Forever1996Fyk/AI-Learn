package com.forever1996Fyk.ai.intelligent.customer.chat.service.impl;

import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.IntelligentCustomerChatAiService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.prompt.PromptService;
import com.forever1996Fyk.ai.intelligent.customer.chat.memory.DatabaseChatMemoryStore;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatApplicationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.rag.config.ElasticSearchConfiguration;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.aggregator.ProgressAwareContentAggregator;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.reranker.BgeScoringModel;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.IntelligentCustomerElasticsearchContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.IntelligentCustomerNeo4jContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.ProgressAwareContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.router.IntelligentCustomerQueryRouter;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.transformer.IntelligentCustomerQueryTransformer;
import dev.langchain4j.community.rag.content.retriever.neo4j.Neo4jGraph;
import dev.langchain4j.community.rag.content.retriever.neo4j.Neo4jText2CypherRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.scoring.onnx.OnnxScoringModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.elasticsearch.ElasticsearchContentRetriever;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationFullText;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 21:53
 **/
@Slf4j
@Service
public class ChatApplicationServiceImpl implements ChatApplicationService {
    @Autowired
    private ChatModel chatModel;
    @Autowired
    private StreamingChatModel streamingChatModel;
    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;
    @Autowired
    private RestClient restClient;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private Driver neo4jDriver;
    @Autowired
    private PromptService promptService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;
    @Autowired
    private DatabaseChatMemoryStore databaseChatMemoryStore;

    @Value("classpath:prompts/text-to-cypher-prompt.txt")
    private Resource textToCypherPrompt;

    @Override
    public String chat(ChatParam chatParam) {
        Result result = getResult(chatParam);
        return result.chatAiService().chat(chatParam.conversationId(), result.question());
    }

    @Override
    public Flux<String> streamChat(ChatParam chatParam) {
        return Flux.<String>create(sink -> {
                    // 进度回调：同时写入 sink 和外部回调
                    Consumer<String> processCallback = sink::next;

                    // 构建问题重写（带进度回调）
                    QueryTransformer queryTransformer = new IntelligentCustomerQueryTransformer(chatModel, chatParam.messageId(), processCallback);

                    // 构建向量检索（带进度回调）
                    ProgressAwareContentRetriever embeddingRetriever = new ProgressAwareContentRetriever(IntelligentCustomerElasticsearchContentRetriever.builder()
                            .configuration(ElasticsearchConfigurationKnn.builder().build())
                            .maxResults(5)
                            .minScore(0.5)
                            .embeddingModel(openAiEmbeddingModel)
                            .restClient(restClient)
                            .indexName(ElasticSearchConfiguration.INDEX_NAME)
                            .knowledgeSegmentService(knowledgeSegmentService)
                            .build(), processCallback);

                    // 构建全文检索（带进度回调）
                    ProgressAwareContentRetriever fullTextRetriever = new ProgressAwareContentRetriever(IntelligentCustomerElasticsearchContentRetriever.builder()
                            .configuration(ElasticsearchConfigurationFullText.builder().build())
                            .maxResults(5)
                            .minScore(0.5)
                            .embeddingModel(openAiEmbeddingModel)
                            .restClient(restClient)
                            .indexName(ElasticSearchConfiguration.INDEX_NAME)
                            .knowledgeSegmentService(knowledgeSegmentService)
                            .build(), processCallback);

                    // 构建图数据检索（带进度回调）
                    ProgressAwareContentRetriever neo4jRetriever = null;
                    try {
                        neo4jRetriever = new ProgressAwareContentRetriever(
                                IntelligentCustomerNeo4jContentRetriever.builder()
                                        .graph(Neo4jGraph.builder()
                                                .driver(neo4jDriver)
                                                .build())
                                        .chatModel(chatModel)
                                        .promptTemplate(new PromptTemplate(textToCypherPrompt.getContentAsString(StandardCharsets.UTF_8)))
                                        .fallbackRetriever(embeddingRetriever)
                                        .build(), processCallback);
                    } catch (IOException e) {
                        log.warn("Error creating Neo4j retriever", e);
                    }

                    // 构建内容聚合（重排序）（带进度回调）
                    ProgressAwareContentAggregator contentAggregator = new ProgressAwareContentAggregator(
                            ReRankingContentAggregator.builder()
                                    .scoringModel(BgeScoringModel.getInstance())
                                    .build(),
                            chatParam.assistantMessageId(),
                            chatMessageService,
                            processCallback
                    );
                    IntentRecognitionResult intentRecognitionResult = chatParam.intentRecognitionResult();
                    // 根据意图识别结果选择提示词模版
                    String prompt = promptService.getPrompt(intentRecognitionResult);
                    // 问题注入
                    ContentInjector contentInjector = DefaultContentInjector.builder()
                            .promptTemplate(PromptTemplate.from(prompt))
                            .build();

                    RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                            .queryRouter(new IntelligentCustomerQueryRouter(List.of(embeddingRetriever, fullTextRetriever, neo4jRetriever), chatModel, processCallback))
                            .queryTransformer(queryTransformer)
                            .contentAggregator(contentAggregator)
                            .contentInjector(contentInjector)
                            .build();
                    IntelligentCustomerChatAiService chatAiService = AiServices.builder(IntelligentCustomerChatAiService.class)
                            .chatModel(chatModel)
                            .streamingChatModel(streamingChatModel)
                            .retrievalAugmentor(retrievalAugmentor)
                            .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                                    .id(memoryId)
                                    .maxMessages(10)
                                    .chatMemoryStore(databaseChatMemoryStore)
                                    .build()
                            )
                            // 这里把意图识别的提示词直接作为系统提示词，来支持多轮对话，防止在多轮对话中，意图识别被重复传给大模型，导致意用户问题被意图识别污染
                            .systemMessage(prompt)
                            .build();

                    // 订阅 LLM 流式输出，桥接到 sink
                    AtomicBoolean firstToken = new AtomicBoolean(true);
                    StringBuilder contentBuilder = new StringBuilder();
                    Disposable disposable = chatAiService.streamChat(chatParam.conversationId(), chatParam.content())
                            .doOnNext((token) -> {
                                // （正常情况下由 ProgressAwareContentAggregator 已发出，此处为兜底）
                                if (firstToken.compareAndSet(true, false)) {
                                    // 标记已开始接收 token
                                }
                                contentBuilder.append(token);
                            })
                            .doOnComplete(() -> chatMessageService.updateContent(chatParam.assistantMessageId(), contentBuilder.toString()))
                            .subscribe(sink::next, sink::error, sink::complete);
                    // 取消时同步取消内部订阅
                    sink.onCancel(disposable::dispose);
                }).subscribeOn(Schedulers.boundedElastic())
                .publishOn(Schedulers.parallel());
    }


    private @NotNull Result getResult(ChatParam chatParam) {
        String question = chatParam.content();
        // 向量检索
        ContentRetriever contentRetriever = IntelligentCustomerElasticsearchContentRetriever.builder()
                .configuration(ElasticsearchConfigurationKnn.builder().build())
                .maxResults(5)
                .minScore(0.5)
                .embeddingModel(openAiEmbeddingModel)
                .restClient(restClient)
                .indexName(ElasticSearchConfiguration.INDEX_NAME)
                .knowledgeSegmentService(knowledgeSegmentService)
                .build();

        // 全文检索
        ContentRetriever fullTextContentRetriever = IntelligentCustomerElasticsearchContentRetriever.builder()
                .configuration(ElasticsearchConfigurationFullText.builder().build())
                .maxResults(5)
                .minScore(0.5)
                .embeddingModel(openAiEmbeddingModel)
                .restClient(restClient)
                .indexName(ElasticSearchConfiguration.INDEX_NAME)
                .knowledgeSegmentService(knowledgeSegmentService)
                .build();

//        // 数据库检索
//        ContentRetriever sqlDatabaseContentRetriever = SqlDatabaseContentRetriever.builder().dataSource(dataSource)
//                .promptTemplate(new PromptTemplate(""))
//                .databaseStructure("")
//                .chatModel(chatModel)
//                .build();

        // 图数据库检索
        ContentRetriever neo4jContentRetriever = Neo4jText2CypherRetriever.builder()
                .graph(
                        Neo4jGraph.builder()
                                .driver(neo4jDriver)
                                .build()
                )
                .chatModel(chatModel)
                .build();
        // 问题改写
        QueryTransformer queryTransformer = new IntelligentCustomerQueryTransformer(chatModel, chatParam.messageId());

        // 问题路由
        QueryRouter queryRouter = new IntelligentCustomerQueryRouter(
                List.of(contentRetriever, fullTextContentRetriever, /*sqlDatabaseContentRetriever,*/ neo4jContentRetriever),
                chatModel);

        // 问题聚合
        ContentAggregator contentAggregator = ReRankingContentAggregator.builder()
                .scoringModel(BgeScoringModel.getInstance())
                .build();

        //意图识别结果
        IntentRecognitionResult intentRecognitionResult = chatParam.intentRecognitionResult();

        // 根据意图识别结果选择提示词模版
        String prompt = promptService.getPrompt(intentRecognitionResult);

        // 问题注入
        ContentInjector contentInjector = DefaultContentInjector.builder()
                .promptTemplate(PromptTemplate.from(prompt))
                .build();

        RetrievalAugmentor retrievalAugmentor = DefaultRetrievalAugmentor.builder()
                .queryRouter(queryRouter)
                .queryTransformer(queryTransformer)
                .contentAggregator(contentAggregator)
                .contentInjector(contentInjector)
                .build();
        IntelligentCustomerChatAiService chatAiService = AiServices.builder(IntelligentCustomerChatAiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .retrievalAugmentor(retrievalAugmentor)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(10)
                        .chatMemoryStore(new InMemoryChatMemoryStore())
                        .build()
                )
                .build();
        return new Result(question, chatAiService);
    }

    private record Result(String question, IntelligentCustomerChatAiService chatAiService) {
    }

}
