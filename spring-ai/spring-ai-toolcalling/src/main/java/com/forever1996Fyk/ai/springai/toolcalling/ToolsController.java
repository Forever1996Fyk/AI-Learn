package com.forever1996Fyk.ai.springai.toolcalling;

import com.forever1996Fyk.ai.springai.toolcalling.tools.MyTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/24 11:10
 **/
@RestController
@RequestMapping("/ai")
public class ToolsController {

    private final ChatClient chatClient;

    public ToolsController(ChatModel chatModel, MyTools tools) {
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem("你是一个非常有帮助的助手，可以使用工具回复用户的问题")
                .defaultTools(tools)
                .build();
    }

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
