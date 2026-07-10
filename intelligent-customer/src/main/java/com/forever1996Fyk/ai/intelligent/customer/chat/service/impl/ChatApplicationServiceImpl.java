package com.forever1996Fyk.ai.intelligent.customer.chat.service.impl;

import com.alibaba.fastjson2.JSON;
import com.forever1996Fyk.ai.intelligent.customer.ai.enums.IntelligentCustomerIntent;
import com.forever1996Fyk.ai.intelligent.customer.ai.model.ChatParam;
import com.forever1996Fyk.ai.intelligent.customer.ai.model.IntentRecognitionResult;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.CommonChatService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.IntelligentCustomerChatAiService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.IntentRecognitionService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.TitleSummaryService;
import com.forever1996Fyk.ai.intelligent.customer.ai.service.prompt.PromptService;
import com.forever1996Fyk.ai.intelligent.customer.business.converter.CarInfoConverter;
import com.forever1996Fyk.ai.intelligent.customer.business.converter.MyCarConverter;
import com.forever1996Fyk.ai.intelligent.customer.chat.enums.ChatSource;
import com.forever1996Fyk.ai.intelligent.customer.chat.memory.DatabaseChatMemoryStore;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.CarInfoEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.repository.bean.MyCarEntity;
import com.forever1996Fyk.ai.intelligent.customer.business.service.CarInfoService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatApplicationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatConversationService;
import com.forever1996Fyk.ai.intelligent.customer.chat.service.ChatMessageService;
import com.forever1996Fyk.ai.intelligent.customer.business.service.MyCarService;
import com.forever1996Fyk.ai.intelligent.customer.document.repository.bean.TableMetaEntity;
import com.forever1996Fyk.ai.intelligent.customer.document.service.KnowledgeSegmentService;
import com.forever1996Fyk.ai.intelligent.customer.document.service.TableMetaService;
import com.forever1996Fyk.ai.intelligent.customer.document.util.DocumentPermissionUtils;
import com.forever1996Fyk.ai.intelligent.customer.rag.config.ElasticSearchConfiguration;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.MetadataKeyConstant;
import com.forever1996Fyk.ai.intelligent.customer.rag.constant.RoleEnum;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.aggregator.ProgressAwareContentAggregator;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.reranker.BgeScoringModel;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.IntelligentCustomerElasticsearchContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.IntelligentCustomerNeo4jContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.IntelligentCustomerSqlDatabaseContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.retriever.ProgressAwareContentRetriever;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.router.IntelligentCustomerQueryRouter;
import com.forever1996Fyk.ai.intelligent.customer.rag.modules.transformer.IntelligentCustomerQueryTransformer;
import dev.langchain4j.community.rag.content.retriever.neo4j.Neo4jGraph;
import dev.langchain4j.community.rag.content.retriever.neo4j.Neo4jText2CypherRetriever;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.aggregator.ContentAggregator;
import dev.langchain4j.rag.content.aggregator.ReRankingContentAggregator;
import dev.langchain4j.rag.content.injector.ContentInjector;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.QueryRouter;
import dev.langchain4j.rag.query.transformer.QueryTransformer;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationFullText;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchConfigurationKnn;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.client.RestClient;
import org.jetbrains.annotations.NotNull;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/7 21:53
 **/
@Slf4j
@Service
public class ChatApplicationServiceImpl implements ChatApplicationService, InitializingBean {
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
    private CommonChatService commonChatService;
    @Autowired
    private ChatMessageService chatMessageService;
    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;
    @Autowired
    private DatabaseChatMemoryStore databaseChatMemoryStore;
    @Autowired
    private ChatConversationService chatConversationService;

    @Autowired
    private MyCarService myCarService;
    @Autowired
    private CarInfoService carInfoService;
    @Autowired
    private TableMetaService tableMetaService;

    @Value("${langchain4j.open-ai.chat-model.api-key}")
    private String chatModelApiKey;

    @Value("${langchain4j.open-ai.chat-model.base-url}")
    private String chatModelBaseUrl;

    @Value("classpath:sql/retrieve_tables.sql")
    private Resource tablesSql;

    @Value("classpath:prompts/text-to-sql-prompt.txt")
    private Resource textToSqlPrompt;

    @Value("classpath:prompts/text-to-cypher-prompt.txt")
    private Resource textToCypherPrompt;

    private IntentRecognitionService intentRecognitionService;

    @Override
    public void afterPropertiesSet() throws Exception {
        intentRecognitionService = AiServices.builder(IntentRecognitionService.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().chatMemoryStore(databaseChatMemoryStore).maxMessages(10).build())
                .build();
    }
    @Override
    public Flux<String> chat(String userId, String content, String conversationId, ChatSource chatSource) {
        // 1. 处理会话：没有 conversationId 则创建新会话
        final String finalConversationId;
        if (!StringUtils.hasText(conversationId)) {
            // 同步：先用 content 前 20 个字符作为临时标题，快速建立回话
            String tempTitle = content.substring(0, Math.min(20, content.length()));
            finalConversationId = chatConversationService.createConversation(userId, tempTitle);
            log.info("创建新会话：conversationId={}, tempTitle={}", finalConversationId, tempTitle);
            // 异步：用虚拟线程调用 LLM 生成摘要标题，完成后回写到数据库
            Thread.ofVirtual().name("title-summary-" + finalConversationId).start(() -> {
                try {
                    // 1. 构建轻量级模型实例
                    OpenAiChatModel titleChatModel = OpenAiChatModel.builder()
                            .apiKey(chatModelApiKey)
                            // 轻量级模型，快速响应
                            .modelName("qwen3.5-flash")
                            // 适度创造性
                            .temperature(0.7)
                            .baseUrl(chatModelBaseUrl)
                            // 关闭思考链，加速响应
                            .customQueryParams(Map.of("enable_thinking", "false"))
                            .build();

                    // 2. 创建 AI Service 代理
                    TitleSummaryService titleSummaryService = AiServices.builder(TitleSummaryService.class)
                            .chatModel(titleChatModel)
                            .build();

                    // 3. 调用 LLM 生成标题
                    String aiTitle = titleSummaryService.generateTitle(content);

                    // 4. 更新数据库
                    chatConversationService.updateTitle(finalConversationId, aiTitle);
                    log.info("异步标题更新完成: conversationId={}, title={}", finalConversationId, aiTitle);
                } catch (Exception e) {
                    log.warn("异步标题生成失败, 保留临时标题: conversationId={}", finalConversationId, e);
                }
            });
        } else {
            finalConversationId = conversationId;
        }

        // 2. 保存用户信息
        String messageId = chatMessageService.saveUserMessage(finalConversationId, content);
        String assistantMessageId = chatMessageService.saveAssistantMessage(finalConversationId);

        // 这里需要再过程输出前面加上类似[PROGRESS]的标签，这样前端才能知道这个输出不是正文而是过程。
        return Flux.just("[PROGRESS]:正在识别您的意图...")
                .concatWith(
                        Mono.fromCallable(() -> intentRecognitionService.chat(finalConversationId, content))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMapMany(intentRecognitionResult -> {
                                    // 意图识别完成后清除缓存，避免意图识别的AI响应污染后续RAG对话的历史记忆
                                    databaseChatMemoryStore.evictCache(finalConversationId);
                                    // 如果用户问题不相关，使用一个通用的LLM做对话
                                    if (!intentRecognitionResult.related()) {
                                        StringBuilder contentBuilder = new StringBuilder();
                                        return Flux.concat(
                                                Flux.just("[PROGRESS]:正在为您生成回答..."),
                                                commonChatService.streamChat(content)
                                                        .doOnNext(token -> contentBuilder.append(token))
                                                        .doOnComplete(() -> chatMessageService.updateContent(assistantMessageId, contentBuilder.toString()))
                                                        .concatWith(Flux.just("[DONE]:" + finalConversationId)));
                                    }

                                    // 5. 相关问题，走RAG流程（进度由内部组件发出）
                                    return ragChat(new ChatParam(userId, finalConversationId, messageId, content, assistantMessageId, intentRecognitionResult, chatSource));
                                })
                )
                .doOnError(e -> log.error("流式对话异常：conversationId={}", finalConversationId))
                // 6. 在流末尾追加一条 [DONE] 事件，携带 conversationId
                .concatWith(Mono.just("[DONE]:" + finalConversationId));
    }

    /**
     * RAG 流式对话。
     * <p>
     * 1. 根据意图识别结果，判断是否需要车辆信息
     * 2. 如果车辆信息不完善，则返回车辆信息不完善提示
     * 3. 根据意图识别结果，判断是否需要车辆信息
     * </p>
     */
    public Flux<String> ragChat(ChatParam chatParam) {
        IntelligentCustomerIntent intent = IntelligentCustomerIntent.getIntent(chatParam.intentRecognitionResult());

        if (chatParam.chatSource() == ChatSource.USER_WEB) {
            // 如果是维保服务、技术支持，则需要车辆信息
            if (intent == IntelligentCustomerIntent.CAR_MAINTENANCE
                    || intent == IntelligentCustomerIntent.CAR_TECH_SUPPORT) {
                if (chatParam.intentRecognitionResult().entities().car_id() == null) {
                    List<MyCarEntity> myCars = myCarService.getCarByUserId(chatParam.userId());
                    if (CollectionUtils.isEmpty(myCars)) {
                        return Flux.just("[WARN]:您还没有添加车辆信息，请先添加车辆信息");
                    } else if (myCars.size() > 1) {
                        return Flux.just("[CARD]:请先选择车辆")
                                .concatWith(Flux.just("[CARD_CHOICE_MYCAR]:" + JSON.toJSONString(MyCarConverter.INSTANCE.toVOList(myCars))));
                    }
                }
            }

            // 如果是营销政策，则需要车辆信息
            if (intent == IntelligentCustomerIntent.CAR_MARKETING) {
                if (chatParam.intentRecognitionResult().entities().car_model() == null) {
                    List<CarInfoEntity> carInfoList = carInfoService.getCarInfoByBrand(null);
                    return Flux.just("[CARD]:请先选择您要咨询的车辆")
                            .concatWith(Flux.just("[CARD_CHOICE_CAR]:" + JSON.toJSONString(CarInfoConverter.INSTANCE.toVOList(carInfoList))));
                }
            }
        }
        return streamChat(chatParam);
    }

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

                    Filter accessibleByFilter = buildFilter(chatParam);

                    // 构建向量检索（带进度回调）
                    ProgressAwareContentRetriever embeddingRetriever = new ProgressAwareContentRetriever(IntelligentCustomerElasticsearchContentRetriever.builder()
                            .configuration(ElasticsearchConfigurationKnn.builder().build())
                            .maxResults(5)
                            .minScore(0.5)
                            .embeddingModel(openAiEmbeddingModel)
                            .restClient(restClient)
                            .indexName(ElasticSearchConfiguration.INDEX_NAME)
                            .knowledgeSegmentService(knowledgeSegmentService)
                            .filter(accessibleByFilter)
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
                            .filter(accessibleByFilter)
                            .build(), processCallback);

                    ProgressAwareContentRetriever sqlRetriever = null;
                    try {
                        // 拼接静态表结构 + table_meta 中动态创建的表结构
                        String databaseStructure = buildDatabaseStructure();
                        sqlRetriever = new ProgressAwareContentRetriever(
                                IntelligentCustomerSqlDatabaseContentRetriever.builder()
                                        .dataSource(dataSource)
                                        .promptTemplate(new PromptTemplate(textToSqlPrompt.getContentAsString(StandardCharsets.UTF_8)))
                                        .databaseStructure(databaseStructure)
                                        .chatModel(chatModel)
                                        .fallbackRetriever(embeddingRetriever)
                                        .build(), processCallback);
                    } catch (IOException e) {
                        log.warn("Error creating SQL retriever", e);
                    }

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
                            .queryRouter(new IntelligentCustomerQueryRouter(List.of(embeddingRetriever, fullTextRetriever, neo4jRetriever, sqlRetriever), chatModel, processCallback))
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

    /**
     * 构建权限过滤器
     *
     * @param chatParam 聊天参数
     * @return 过滤器
     */
    private Filter buildFilter(ChatParam chatParam) {
        // 默认权限过滤器：允许访客权限
        Filter permissionFilter = MetadataFilterBuilder.metadataKey(MetadataKeyConstant.ACCESSIBLE_BY).isEqualTo(RoleEnum.VISITOR.name());

        // 根据用户角色获取权限 todo
        RoleEnum roleEnum = null;

        // 获取该文档支持的所有权限
        String[] permissions = DocumentPermissionUtils.getDocumentAccessiblePermission(roleEnum);
        for (String permission : permissions) {
            // 非访客权限时，将权限用 or 连接，表示支持多种权限
            if (!RoleEnum.VISITOR.name().equals(permission)) {
                permissionFilter = permissionFilter.or(MetadataFilterBuilder.metadataKey(MetadataKeyConstant.ACCESSIBLE_BY).isEqualTo(permission));
            }
        }
        return permissionFilter;
    }

    /**
     * 构建数据库结构描述
     * <p>
     * 将静态表(也就是业务表)结构（retrieve_tables.sql）与 table_meta 表(这里的表结构为文档上传时创建的表)中动态创建的表结构合并，
     * 作为 Text2SQL Prompt 的 databaseStructure 参数，使 LLM 感知所有可查询的表。
     */
    private String buildDatabaseStructure() throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(tablesSql.getContentAsString(StandardCharsets.UTF_8));

        // 从 table_meta 读取当前激活版本对应的动态表结构
        List<TableMetaEntity> tableMetas = tableMetaService.listActiveForQuery();
        if (CollectionUtils.isNotEmpty(tableMetas)) {
            stringBuilder.append("\n\n");
            String dynamicSql = tableMetas.stream()
                    .filter(meta -> meta.getCreateSql() != null && !meta.getCreateSql().isBlank())
                    .map(TableMetaEntity::getCreateSql)
                    .collect(Collectors.joining("\n\n"));
            stringBuilder.append(dynamicSql);
        }
        return stringBuilder.toString();
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
