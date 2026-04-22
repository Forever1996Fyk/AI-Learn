package com.forever1996Fyk.ai.springai.example.deepseek.controller;

import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/22 22:19
 **/
@RestController
@RequestMapping("/dp/ai")
public class DpChatController {

    @Autowired
    private DeepSeekChatModel chatModel;

    /**
     * 获取deepseek chat的模型结果
     *
     * @param message message
     * @return response
     */
    @GetMapping("/generate")
    public String generate(@RequestParam(value = "message", defaultValue = "你是谁?") String message) {
        String response = chatModel.call(message);
        return response;
    }
}
