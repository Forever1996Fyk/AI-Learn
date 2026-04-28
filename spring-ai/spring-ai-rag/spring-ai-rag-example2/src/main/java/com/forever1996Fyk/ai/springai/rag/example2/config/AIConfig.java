package com.forever1996Fyk.ai.springai.rag.example2.config;

import io.milvus.client.MilvusClient;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.param.R;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.response.GetCollStatResponseWrapper;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
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
    public void initVectorData() throws TikaException, IOException {
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

        // 加载外部文件数据到向量数据库
        loadAndStoreDocumentData();

        milvusClient.flush(
                FlushParam.newBuilder()
                        .withCollectionNames(List.of(collectionName)).build()
        );
    }

    /**
     * 读取 word 文档内容，存入 Milvus
     */
    private void loadAndStoreDocumentData() throws IOException, TikaException {
        ClassPathResource resource = new ClassPathResource("");
        Tika tika = new Tika();
        String text = tika.parseToString(resource.getFile());

        // 拆分文档
        // 使用 TokenTextSplitter 拆分文本
        TokenTextSplitter splitter = TokenTextSplitter.builder()
                // 每块大小 800
                .withChunkSize(800)
                // 最小块的字符数 400
                .withMinChunkSizeChars(400)
                // 保留分隔符
                .withKeepSeparator(true)
                .build();
        List<Document> chunks = splitter.apply(List.of(new Document(text)));

        // 写入向量数据库
        vectorStore.add(chunks);
    }
}
