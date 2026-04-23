package com.forever1996Fyk.ai.springai.chatclient.configuration;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.zhipuai.ZhiPuAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/23 18:05
 **/
@Configuration
public class ChatClientConfiguration {

    @Bean
    public ChatClient deepseekClient(DeepSeekChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("你是一个人工智能助手，你的名字叫做小智")
                .build();
    }

    @Bean
    public ChatClient zhipuaiClient(ZhiPuAiChatModel chatModel) {
        // 两种创建方式都可
        return ChatClient.create(chatModel);
    }
}
