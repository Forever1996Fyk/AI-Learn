package com.forever1996Fyk.ai.springai.rag.example2;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/4/28 11:02
 **/
@RestController
@RequestMapping("/ai")
public class RagController {
    @Autowired
    private ChatClient chatClient;

    @Autowired
    private QuestionAnswerAdvisor questionAnswerAdvisor;
    @Autowired
    private RetrievalAugmentationAdvisor retrievalAugmentationAdvisor;

    @GetMapping("/chat1")
    public String chat1(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(questionAnswerAdvisor)
                .call()
                .content();
    }

    @GetMapping("/chat2")
    public String chat2(@RequestParam String message) {
        return chatClient.prompt()
                .user(message)
                .advisors(retrievalAugmentationAdvisor)
                .call()
                .content();
    }
}
