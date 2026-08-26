package com.forever1996Fyk.ai.aiframework.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.session.JsonSession;
import io.agentscope.core.session.Session;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 23:28
 **/
public class PersistentChatDemo {
    private final static String api_key = System.getenv("dashscope.api-key");

    public static void main(String[] args) {
        String sessionId = "user_yk_session";

        // 1. 创建 Session（JSON文件持久化）
        Path sessionPath = Paths.get(System.getProperty("user.home"),
                ".agentscope", "examples", "sessions");
        Session session = new JsonSession(sessionPath);

        // 2. 创建 Agent 组件
        InMemoryMemory memory = new InMemoryMemory();

        ReActAgent agent = ReActAgent.builder()
                .name("Assistant")
                .sysPrompt("You are a helpful AI assistant with persistent memory. ")
                .model(DashScopeChatModel.builder()
                        .apiKey(api_key)
                        .modelName("qwen-max")
                        .build())
                .memory(memory)
                .build();

        // 3. 如果之前有保存的会话，加载它（恢复历史上下文）
        boolean resumed = agent.loadIfExists(session, sessionId);
        if (resumed) {
            System.out.println("Session restored! " + memory.getMessages().size() + " messages loaded.");
        } else {
            System.out.println("New session started.");
        }

        // 4. 发送新消息（延续之前的对话上下文）
//        Msg userMsg = Msg.builder()
//                .role(MsgRole.USER)
//                .content(TextBlock.builder().text("My name is Hollis and I'm a software engineer.").build())
//                .build();

        // 第二轮对话
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder().text("What's my name and what do I do?").build())
                .build();

        Msg response = agent.call(userMsg).block();
        System.out.println("Agent: " + response.getTextContent());

        // 5. 保存会话（下次启动时可恢复）
        agent.saveTo(session, sessionId);
        System.out.println("Session saved. Messages in memory: " + memory.getMessages().size());
    }
}
