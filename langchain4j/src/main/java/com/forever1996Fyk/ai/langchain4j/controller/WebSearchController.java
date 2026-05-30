package com.forever1996Fyk.ai.langchain4j.controller;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/30 23:16
 **/
@RestController
@RequestMapping("/websearch")
public class WebSearchController {
    @Autowired
    OpenAiChatModel chatModel;

    @GetMapping("/search")
    public String webSearch(String query) {
        // 1. 配置搜索引擎
        TavilyWebSearchEngine searchEngine = TavilyWebSearchEngine.builder()
                .apiKey(System.getenv("tavily.api.key"))
                .includeAnswer(true)
                .searchDepth("advanced")
                .build();

        // 2. 配置 Web 搜索检索器
        WebSearchContentRetriever webRetriever = WebSearchContentRetriever.builder()
                .webSearchEngine(searchEngine)
                .maxResults(5)
                .build();

        // 3. 配置 RetrievalAugmentor
        DefaultRetrievalAugmentor augmentor = DefaultRetrievalAugmentor.builder()
                .contentRetriever(webRetriever)
                .build();

        // 5. 创建 AI Service
        interface WebSearchAssistant {
            String chat(String userMessage);
        }

        WebSearchAssistant assistant = AiServices.builder(WebSearchAssistant.class)
                .chatModel(chatModel)
                .retrievalAugmentor(augmentor)
                .build();

        // 6. 使用 - 获取实时信息
        return assistant.chat(query);
    }
}
