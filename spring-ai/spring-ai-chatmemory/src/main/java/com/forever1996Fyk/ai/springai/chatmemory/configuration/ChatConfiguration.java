package com.forever1996Fyk.ai.springai.chatmemory.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/24 09:34
 **/
@Configuration
public class ChatConfiguration {

//    /**
//     * 创建 ChatMemory Bean。且将聊天记忆存在内存中
//     *
//     * @return
//     */
//    @Bean
//    public ChatMemory chatMemory() {
//        // 这里没有设置 ChatMemoryRepository，默认使用内存存储 InMemoryChatMemoryRepository
//        return MessageWindowChatMemory.builder()
//                .maxMessages(20)
//                .build();
//    }

    /**
     * 创建 ChatMemory Bean。且将聊天记忆存在Jdbc中
     *
     * @return
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository repository) {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .chatMemoryRepository(repository)
                .build();
    }

    /**
     * 创建 ChatClient Bean，并注入 ChatModel与 ChatMemory
     *
     * @param chatModel
     * @param chatMemory
     * @return
     */
    @Bean
    public ChatClient chatClient(DeepSeekChatModel chatModel, ChatMemory chatMemory) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }
}
