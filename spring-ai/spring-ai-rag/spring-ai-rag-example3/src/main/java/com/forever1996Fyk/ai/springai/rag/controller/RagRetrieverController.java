package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.forever1996Fyk.ai.springai.rag.embedding.EmbeddingService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/27 21:43
 **/
@RestController
@RequestMapping("/rag/retriever")
public class RagRetrieverController implements InitializingBean {
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private PgVectorStore vectorStore;
    @Autowired
    private DashScopeChatModel chatModel;

    private ChatClient chatClient;

    @GetMapping("/retrieve")
    public String retrieve(String query, double threshold) {
        List<Document> documents = embeddingService.similaritySearch(SearchRequest
                .builder()
                .query(query).similarityThreshold(threshold).build());

        String documentContent = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n=========文档分隔线===========\n\n"));
        // 2. 构建提示词模板
        String promptTemplate = """
            请基于以下提供的参考文档内容，回答用户的问题。
            如果参考文档中没有相关信息，请直接说明"没有找到相关信息"，不要编造内容。
            
            参考文档:
            {documents}
            
            用户问题: {question}
            """;
        PromptTemplate prompt = new PromptTemplate(promptTemplate);
        Prompt realPrompt = prompt.create(Map.of("documents", documentContent, "question", query));
        return chatModel.call(realPrompt).getResult().getOutput().getText();
    }

    @GetMapping("/get")
    public String getRetriever(String query) {
        // 这里可能会查不到信息，原因是similarityThreshold 太大了，我这里默认是 0.75 所以查不到，尝试改小点即可。
        List<Document> documents = embeddingService.similaritySearch(query);
        StringBuilder sb = new StringBuilder();
        for (Document document : documents) {
            sb.append(document.getText()).append("\n");
            sb.append("=============================");
        }
        return sb.toString();
    }

    @GetMapping("/retrieveAdvisor")
    public String retrieveAdvisor(String query) {
        return chatClient.prompt(query).call().content();
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        // 自定义Prompt模板
        PromptTemplate promptTemplate = new PromptTemplate("""
                请基于以下提供的参考文档内容，回答用户的问题。
                如果参考文档中没有相关信息，请直接说明"没有找到相关信息"，不要编造内容。
                                
                参考文档内容:
                {question_answer_context}
                                
                用户问题: {query}
                """);
        QuestionAnswerAdvisor questionAnswerAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(
                        SearchRequest.builder().similarityThreshold(0.5f).topK(5).build()
                ).promptTemplate(promptTemplate).build();
        this.chatClient = ChatClient.builder(chatModel)
                // 实现 Logger 的 Advisor
                .defaultAdvisors(questionAnswerAdvisor)
                // 设置 ChatClient 中 ChatModel 的 Options 参数
                .defaultOptions(
                        DashScopeChatOptions.builder()
                                .topP(0.7)
                                .build()
                ).build();
    }
}
