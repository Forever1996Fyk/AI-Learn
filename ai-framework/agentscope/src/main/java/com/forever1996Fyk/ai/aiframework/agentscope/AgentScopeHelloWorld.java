package com.forever1996Fyk.ai.aiframework.agentscope;

import com.forever1996Fyk.ai.aiframework.agentscope.tools.SimpleTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Toolkit;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 21:57
 **/
public class AgentScopeHelloWorld {

    public static void main(String[] args) {
        // 准备工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SimpleTools());

        ReActAgent agent = ReActAgent.builder()
                .name("Jarvis")
                .sysPrompt("你是一个名为 Jarvis 的助手")
                .model(DashScopeChatModel.builder()
                        .apiKey(System.getenv("dashscope.api-key"))
                        .modelName("qwen3-max")
                        .build())
                .toolkit(toolkit)
                .build();

        // 发送消息
        Msg msg = Msg.builder()
                .textContent("你好！Jarvis，现在几点了")
                .build();
        Msg response = agent.call(msg).block();
        System.out.println(response.getTextContent());
    }
}
