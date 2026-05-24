package com.forever1996Fyk.ai.springai.example.controller;

import com.forever1996Fyk.ai.springai.example.entity.ChatStatus;
import com.forever1996Fyk.ai.springai.example.entity.OrderChat;
import com.forever1996Fyk.ai.springai.example.tools.OrderTools;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/23 17:16
 **/
@RestController
@RequestMapping("/pdd/refund")
public class PddRefundController {
    @Autowired
    private ChatClient chatClient;

    @GetMapping("/newChat")
    public OrderChat newChat(String userId, String orderId, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        String chatId = UUID.randomUUID().toString();
        return chatClient.prompt()
                .user(String.format("我要咨询订单相关的售后问题，我的用户id是%s,我的订单号是: %s ,本地的对话Id是 %s，当前状态是 %s", userId, orderId, chatId, ChatStatus.CHAT_START.name()))
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param("chat_memory_retrieve_size", 100)
                ).call().entity(OrderChat.class);

    }

    @Autowired
    private OrderTools orderTools;
    @Value("classpath:prompts/pdd_refund_system_prompt.st")
    private Resource systemPrompt;

    @GetMapping("/ask")
    public Flux<String> ask(String question, String chatId, HttpServletResponse response) {
        PromptTemplate promptTemplate = PromptTemplate.builder().resource(systemPrompt).build();
        response.setCharacterEncoding("UTF-8");
        return chatClient.prompt(promptTemplate.create())
                .user(question).tools(orderTools)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param("chat_memory_retrieve_size", 100)
                )
                .stream().content();
    }
}
