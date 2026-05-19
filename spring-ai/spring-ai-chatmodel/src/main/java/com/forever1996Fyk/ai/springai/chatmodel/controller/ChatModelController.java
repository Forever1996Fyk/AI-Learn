package com.forever1996Fyk.ai.springai.chatmodel.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/3 17:02
 **/
@RestController
@RequestMapping("/call")
public class ChatModelController {
    @Autowired
    private DashScopeChatModel chatModel;

    @GetMapping("/string")
    public String callString(String message) {
        return chatModel.call(message);
    }

    @GetMapping("/stream")
    public Flux<String> callStream(String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        return chatModel.stream(message);
    }

    @GetMapping("/prompt")
    public String callPrompt(String message) {
        SystemMessage systemMessage = new SystemMessage("请如实回答我的问题");
        UserMessage userMessage = new UserMessage(message);

        ChatOptions chatOptions = ChatOptions.builder().model("deepseek-v3").build();
        Prompt prompt = new Prompt.Builder().messages(systemMessage, userMessage).chatOptions(chatOptions).build();
        return chatModel.call(prompt).getResult().getOutput().getText();
    }
}
