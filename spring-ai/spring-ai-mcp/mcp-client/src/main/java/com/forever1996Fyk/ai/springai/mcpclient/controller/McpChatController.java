package com.forever1996Fyk.ai.springai.mcpclient.controller;

import com.forever1996Fyk.ai.springai.mcpclient.service.ManualMcpClientService;
import com.forever1996Fyk.ai.springai.mcpclient.service.McpClientService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/25 11:09
 **/
@RestController
@RequestMapping("/mcp")
public class McpChatController {
    @Autowired
    private ManualMcpClientService mcpClientService;

    @GetMapping("/chat")
    public String chat(@RequestParam String message) {
        return mcpClientService.chat(message);
    }
}
