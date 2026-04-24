package com.forever1996Fyk.ai.springai.chatmemory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/24 10:35
 **/
@Component
public class Tools {

    @Tool(description = "获取当前日期和时间")
    public String getCurrentDateTime() {
        return "";
    }

    @Tool(description = "设置闹钟")
    public void setAlarm(@ToolParam(description = "闹钟时间") String alarmTime) {

    }

    public static void main(String[] args, ChatModel chatModel) {
        //使用 ChatClient注册 工具
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个非常有帮助的助手，可以使用工具来帮助回答问题")
                .defaultTools(new Tools(), new Tools())
                .build();
        // 调用大模型
        String result = chatClient.prompt()
                .user("给我设置10分钟后的闹钟")
                .call()
                .content();
    }
}
