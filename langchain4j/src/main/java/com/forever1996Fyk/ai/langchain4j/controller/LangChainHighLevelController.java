package com.forever1996Fyk.ai.langchain4j.controller;

import com.forever1996Fyk.ai.langchain4j.chatmemory.RedisChatMemoryStore;
import com.forever1996Fyk.ai.langchain4j.entity.Book;
import com.forever1996Fyk.ai.langchain4j.service.LangChainAiService;
import com.forever1996Fyk.ai.langchain4j.service.LangChainMemoryAiService;
import com.forever1996Fyk.ai.langchain4j.tool.TemperatureTools;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.spring.AiService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/22 21:19
 **/
@RestController
@RequestMapping("/langchain/high")
public class LangChainHighLevelController implements InitializingBean {
    @Autowired
    private OpenAiChatModel chatModel;
    @Autowired
    private StreamingChatModel streamingChatModel;
    @Autowired
    private LangChainAiService aiService;


    @GetMapping("/chat")
    public String chat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.chat("日本都有哪些美食？");
    }

    @GetMapping("/streamChat")
    public Flux<String> streamChat(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.streamChat("日本都有哪些美食？");
    }

    @GetMapping("/chatTemplate")
    public Flux<String> chatTemplate(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.chatTemplate("我饿了");
    }

    @GetMapping("/getBooks")
    public Book getBooks(HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return aiService.getBooks();
    }

    @GetMapping("/chatMemory")
    public String chatMemory(String memoryId, String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return langChainMemoryAiService.chatMemory(memoryId, message);
    }

    @GetMapping("/toolCalling")
    public String toolCalling(String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        LangChainAiService langChainAiService = AiServices.builder(LangChainAiService.class)
                .tools(new TemperatureTools())
                .chatModel(chatModel)
                .build();
        return langChainAiService.chat(message);
    }

    @Autowired
    private RedisChatMemoryStore redisChatMemoryStore;

    private LangChainMemoryAiService langChainMemoryAiService;
    @Override
    public void afterPropertiesSet() throws Exception {
        langChainMemoryAiService = AiServices.builder(LangChainMemoryAiService.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(10))
                .build();
//        langChainMemoryAiService = AiServices.builder(LangChainMemoryAiService.class)
//                .chatModel(chatModel)
//                .streamingChatModel(streamingChatModel)
//                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder().id(memoryId).maxMessages(10).chatMemoryStore(redisChatMemoryStore).build())
//                .build();

    }
}
