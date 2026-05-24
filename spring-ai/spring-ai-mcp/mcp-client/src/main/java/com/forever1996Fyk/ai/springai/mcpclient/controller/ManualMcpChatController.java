package com.forever1996Fyk.ai.springai.mcpclient.controller;

import com.forever1996Fyk.ai.springai.mcpclient.service.ManualMcpClientService;
import com.forever1996Fyk.ai.springai.mcpclient.service.McpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/25 11:09
 **/
@RestController
@RequestMapping("/mcp")
public class ManualMcpChatController {
    @Autowired
    private ManualMcpClientService mcpClientService;

    @GetMapping("/manualChat")
    public String chat(@RequestParam String message) {
        return mcpClientService.chat(message);
    }
}
