package com.forever1996Fyk.ai.springai.chatmemory;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.memory.mem0.advisor.Mem0ChatMemoryAdvisor;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static com.alibaba.cloud.ai.memory.mem0.advisor.Mem0ChatMemoryAdvisor.USER_ID;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/16 22:22
 **/
@RestController
@RequestMapping("/longTermMemory")
public class LongTermMemoryController implements InitializingBean {
    @Resource
    private DashScopeChatModel chatModel;

    private ChatClient chatClient;
    @Autowired
    private VectorStore mem0MemoryStore;
    @RequestMapping("/chat")
    public String chat(String message, String userId) {
        return chatClient.prompt(message)
                .advisors(
                        req -> req.params(Map.of(USER_ID, userId))
                )
                .call().content();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Mem0ChatMemoryAdvisor mem0ChatMemoryAdvisor = Mem0ChatMemoryAdvisor.builder(mem0MemoryStore).build();
        this.chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(mem0ChatMemoryAdvisor)
                .build();
    }

}
