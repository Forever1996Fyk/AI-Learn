package com.forever1996Fyk.ai.springai.prompt.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.template.TemplateRenderer;
import org.springframework.ai.template.st.StTemplateRenderer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/5/19 22:56
 **/
@RestController
@RequestMapping("/prompt")
public class PromptTemplateController {
    private final ChatClient chatClient;

    public PromptTemplateController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/template")
    public String call(String topic) {
        String template = """
                请给我推荐几个关于{topic}的开源项目
                """;
        PromptTemplate promptTemplate = new PromptTemplate(template);
        promptTemplate.add("topic", topic);
        return chatClient.prompt(promptTemplate.create()).call().content();
    }

    @GetMapping("/streamTemplate")
    public Flux<String> streamTemplate(String topic, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        String template = """
                请给我推荐几个关于{topic}的开源项目
                """;
        PromptTemplate promptTemplate = new PromptTemplate(template);
        return chatClient.prompt(promptTemplate.create(Map.of("topic", topic)))
                .stream().content();
    }

    @Value("classpath:/templates/open_source_system_prompt.st")
    private Resource template;

    @GetMapping("/fileTemplate")
    public Flux<String> chat2(@RequestParam(value = "message") String message, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");

        HashMap variables = new HashMap();
        variables.put("language", "Java");
        variables.put("topic", message);
        PromptTemplate promptTemplate = PromptTemplate.builder().resource(template).variables(variables).build();

        return chatClient.prompt(promptTemplate.create(Map.of("topic", message))).system("你是一个专业的的github项目收集人员").stream().content();
    }

    @GetMapping("/changeDelimiter")
    public Flux<String> changeDelimiter(String topic, HttpServletResponse response) {
        response.setCharacterEncoding("UTF-8");
        String template = """
                请给我推荐几个关于{topic}的开源项目
                """;
        PromptTemplate promptTemplate = PromptTemplate.builder()
                .renderer(
                        StTemplateRenderer.builder()
                                .startDelimiterToken('<')
                                .endDelimiterToken('>')
                                .build()
                )
                .template(template).build();
        return chatClient.prompt(promptTemplate.create(Map.of("topic", topic)))
                .stream().content();
    }
}
