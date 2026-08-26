package com.forever1996Fyk.ai.aiframework.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 23:15
 **/
public class MultiTurnChatDemo {

    private final static String api_key = System.getenv("dashscope.api-key");


    public static void main(String[] args) {
        // 创建 Memory（负责维护会话历史）
        InMemoryMemory memory = new InMemoryMemory();

        // 创建 Agent
        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant. Remember what the user tells you.")
                .model(DashScopeChatModel.builder()
                        .apiKey(api_key)
                        .modelName("qwen-max")
                        .build())
                // 注入 Memory
                .memory(memory)
                .build();

        // === 第1轮 ===
        Msg msg1 = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("My name is YK and I'm a software engineer.").build())
                .build();
        Msg reply1 = agent.call(msg1).block();
        System.out.println("Agent: " + reply1.getTextContent());


        // === 第2轮（Agent 能记住第1轮信息）===
        Msg msg2 = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("What's my name and what do I do?").build())
                .build();
        Msg reply2 = agent.call(msg2).block();
        System.out.println("Agent: " + reply2.getTextContent());

        // Agent 会回答: "Your name is YK and you're a software engineer."

        // 查看 Memory 中的完整对话历史
        System.out.println("Total messages in memory: " + memory.getMessages().size());
        // 输出: 4（user1 + assistant1 + user2 + assistant2）
    }
}
