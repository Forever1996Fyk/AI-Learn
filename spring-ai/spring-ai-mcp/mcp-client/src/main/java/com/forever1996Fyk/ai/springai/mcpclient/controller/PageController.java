package com.forever1996Fyk.ai.springai.mcpclient.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/28 17:28
 **/
@Controller
public class PageController {

    @GetMapping("/chat")
    public String chat() {
        return "chat";
    }
}
