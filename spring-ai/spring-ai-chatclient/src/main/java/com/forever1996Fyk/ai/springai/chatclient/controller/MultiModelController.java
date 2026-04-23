package com.forever1996Fyk.ai.springai.chatclient.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/23 18:10
 **/
@RestController
@RequestMapping("/ai")
public class MultiModelController {
    @Qualifier("deepseekClient")
    @Autowired
    private ChatClient deepseekClient;
    @Qualifier("zhipuaiClient")
    @Autowired
    private ChatClient zhipuaiClient;

    /**
     * 与 DeepSeek聊天
     */
    @GetMapping("/chatWithDeepseek")
    public String chatWithDeepseek(@RequestParam String message) {
        return deepseekClient.prompt()
                .user(message)
                .call()
                .content();
    }

    /**
     * 与 Zhipuai聊天
     */
    @GetMapping("/chatWithZhipuai")
    public String chatWithZhipuai(@RequestParam String message) {
        return zhipuaiClient.prompt()
                .user(message)
                .call()
                .content();
    }
}
