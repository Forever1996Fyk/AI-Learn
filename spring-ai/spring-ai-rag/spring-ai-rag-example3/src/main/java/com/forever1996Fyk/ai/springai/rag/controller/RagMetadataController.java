package com.forever1996Fyk.ai.springai.rag.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.forever1996Fyk.ai.springai.rag.embedding.EmbeddingService;
import com.forever1996Fyk.ai.springai.rag.reader.factory.DocumentReaderStrategyFactory;
import com.forever1996Fyk.ai.springai.rag.service.DocumentCleaner;
import com.forever1996Fyk.ai.springai.rag.splitter.OverlapParagraphTextSplitter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/28 21:50
 **/
@RestController
@RequestMapping("/rag/metadata")
public class RagMetadataController implements InitializingBean {
    @Autowired
    private EmbeddingService embeddingService;
    @Autowired
    private DocumentReaderStrategyFactory documentReaderStrategyFactory;

    @Autowired
    private DashScopeChatModel chatModel;

    private ChatClient chatClient;

    @GetMapping("/embedding")
    public String embedding(String filePath, String fileName) {
        List<Document> documents;
        try {
            documents = documentReaderStrategyFactory.read(new File(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        for (Document document : documents) {
            document.getMetadata().put("fileName", fileName);
        }
        embeddingService.embedAndStore(documents);
        return "success";
    }

    @GetMapping("/retrieve")
    public List<Document> retrieveMetadata(String query, String fileName) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .filterExpression("fileName == '" + fileName + "'").build();
        return embeddingService.similaritySearch(request);
    }

    @GetMapping("/retrieveAdvisorWithMetadata")
    public String retrieveAdvisorWithMetadata(String query, String fileName) {
        return chatClient.prompt(query)
                .advisors(advisorSpec -> advisorSpec.param("qa_filter_expression", "fileName == '" + fileName + "'"))
                .call().content();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.chatClient = ChatClient.builder(chatModel)
                .build();
    }
}
