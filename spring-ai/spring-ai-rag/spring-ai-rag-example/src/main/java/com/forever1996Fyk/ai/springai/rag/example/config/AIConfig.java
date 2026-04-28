package com.forever1996Fyk.ai.springai.rag.example.config;

import io.milvus.client.MilvusClient;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.param.R;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.response.GetCollStatResponseWrapper;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @program: AI-Learn
 * @description: 1. 构建 ChatClient
 * 2. 构建 QuestionAnswerAdvisor
 * 3. 构建 RetrievalAugmentationAdvisor
 * 4. 向向量数据库中写入数据，项目启动写入一次即可
 * @author: YuKai Fan
 * @create: 2026/4/28 10:30
 **/
@Configuration
public class AIConfig {

    @Autowired
    private MilvusVectorStore vectorStore;

    @Value("${spring.ai.vectorstore.milvus.collection-name}")
    private String collectionName;

    @Bean
    public ChatClient chatClient(DeepSeekChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个助手，回答用户问题时不要提及是从向量数据库中获取，如果你对用户问题不知道，请直接回复，不知道这个问题的答案")
                .build();
    }

    @Bean
    public QuestionAnswerAdvisor questionAnswerAdvisor() {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder()
                                // 相似阈值
                                .similarityThreshold(0.5)
                                .topK(6)
                                .build()
                ).build();
    }

    @Bean
    public RetrievalAugmentationAdvisor retrievalAugmentationAdvisor() {
        VectorStoreDocumentRetriever retriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5)
                .topK(6)
                .build();

        // 扩展上下文，作用与queryExpander类似
        ContextualQueryAugmenter augmenter = ContextualQueryAugmenter.builder()
                // 允许上下文为空
                .allowEmptyContext(true)
                .build();
        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(retriever)
                .queryAugmenter(augmenter)
                // 设置上下文，比如 设置提示词：你是个 xxx 专家....
//                .queryExpander()
                .build();
    }

    /**
     * 向向量数据库中写入数据
     */
    @PostConstruct
    public void initVectorData() {
        System.out.println("初始化向量数据，写入到 Milvus 中...");

        // 获取 Milvus 客户端
        MilvusClient milvusClient = (MilvusClient) vectorStore.getNativeClient().get();

        // 先获取 collection的数据，如果大于 0，则不写入，否则写入
        R<GetCollectionStatisticsResponse> resp = milvusClient.getCollectionStatistics(
                GetCollectionStatisticsParam.newBuilder().withCollectionName(collectionName).build()
        );
        long rowCount = new GetCollStatResponseWrapper(resp.getData()).getRowCount();
        System.out.println("Milvus vector store 中的数据量：" + rowCount);

        if (rowCount > 0) {
            return;
        }
        System.out.println("开始写入数据");
        List<Document> documents = List.of(
                Document.builder().text("Spring AI 是一个开源的 AI 集成项目").build(),
                Document.builder().text("Milvus是一款高性能的向量数据库").build(),
                Document.builder().text("DeepSeek 是一个开源大语言模型").build()
        );
        // vectorStore.add，Spring AI的 VectorStore 会使用注入的 Embedding Model 将 Document 向量化并写入 Milvus
        vectorStore.add(documents);

        milvusClient.flush(
                FlushParam.newBuilder()
                        .withCollectionNames(List.of(collectionName)).build()
        );
    }
}
