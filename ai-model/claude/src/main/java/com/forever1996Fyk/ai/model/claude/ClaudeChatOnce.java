package com.forever1996Fyk.ai.model.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.anthropic.models.messages.TextBlock;

import java.util.List;
import java.util.Optional;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/20 11:33
 **/
public class ClaudeChatOnce {

    public static void main(String[] args) {

        // 创建Anthropic客户端，使用 API KEY 认证
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(Config.getApiKey())
                .build();

        // 构建消息创建参数，设置最大令牌数，用户消息和模型
        MessageCreateParams params = MessageCreateParams.builder()
                .maxTokens(1024L)
                .addUserMessage("你好，claude")
                .model(Model.CLAUDE_HAIKU_4_5)
                .build();

        // 发送消息并获取回复
        Message message = client.messages().create(params);

        System.out.println(message);

        System.out.println("===============");

        // 提取并打印回复的文本内容
        List<ContentBlock> content = message.content();
        for (ContentBlock contentBlock : content) {
            Optional<TextBlock> text = contentBlock.text();
            if (text.isPresent()) {
                System.out.println(text.get().text());
            }
        }
    }
}
