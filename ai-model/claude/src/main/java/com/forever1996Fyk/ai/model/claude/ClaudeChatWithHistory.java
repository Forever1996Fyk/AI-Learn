package com.forever1996Fyk.ai.model.claude;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;

import java.util.Scanner;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/20 11:45
 **/
public class ClaudeChatWithHistory {

    public static void main(String[] args) {
        // 创建Anthropic客户端，使用 API KEY 认证
        AnthropicClient client = AnthropicOkHttpClient.builder()
                .apiKey(Config.getApiKey())
                .build();

        // 创建一个 Scanner 对象以读取用户输入
        Scanner scanner = new Scanner(System.in);
        System.out.println("与 Claude 的对话开始，输入 ‘退出’ 结束对话。");

        // 初始化 参数
        MessageCreateParams.Builder builder = MessageCreateParams.builder()
                .model(Model.CLAUDE_HAIKU_4_5)
                .maxTokens(1024L);

        // 循环读取用户输入并发送消息，知道用户输入“退出”
        while (true) {
            System.out.println("你：");
            String userInput = scanner.nextLine();
            if ("退出".equalsIgnoreCase(userInput.trim())) {
                System.out.println("对话结束");
                break;
            }
            // 添加用户信息
            builder.addUserMessage(userInput);

            MessageCreateParams params = builder.build();
            // 发送消息并获取回复
            Message message = client.messages().create(params);
            // 提取并打印回复的文本内容
            String replay = message.content().get(0).text().get().text();
            System.out.println("Claude: " + replay);
        }
    }
}
