package com.forever1996Fyk.ai.aiframework.agentscope.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.memory.InMemoryMemory;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolEmitter;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.Toolkit;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 23:01
 **/
public class InterruptionDemo {

    private final static String api_key = System.getenv("dashscope.api-key");

    public static void main(String[] args) throws InterruptedException {
        // 注册一个耗时工具
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new SlowTools());

        ReActAgent agent = ReActAgent.builder()
                .name("DataAgent")
                .sysPrompt("You are a data processing assistant. "
                        + "Use the process_large_dataset tool to process datasets.")
                .model(DashScopeChatModel.builder()
                        .apiKey(api_key)
                        .modelName("qwen-max")
                        .stream(false)
                        .build()
                )
                .toolkit(toolkit)
                .memory(new InMemoryMemory())
                .maxIters(10)
                .build();

        // 用户请求
        Msg userMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder()
                        .text("Process the 'orders' dataset with 'aggregate' operation.")
                        .build())
                .build();

        // 在单独线程启动 Agent
        Thread agentThread = new Thread(() -> {
            Msg response = agent.call(userMsg).block();
            System.out.println("[Agent] " + response.getTextContent());
        });
        agentThread.start();

        // 等 2 秒后中断 Agent
        Thread.sleep(2000);
        System.out.println(">>> USER INTERRUPTS <<<");

        // 携带中断消息（可选）
        Msg interruptMsg = Msg.builder()
                .role(MsgRole.USER)
                .content(TextBlock.builder()
                        .text("Stop! I need to change parameters.")
                        .build())
                .build();
        agent.interrupt(interruptMsg);

        agentThread.join();
        System.out.println("Memory size: " + agent.getMemory().getMessages().size());
    }

    // 模拟耗时工具
    public static class SlowTools {
        @Tool(name = "process_large_dataset",
                description = "Process a large dataset (takes a long time)")
        public String processLargeDataset(
                @ToolParam(name = "dataset_name") String name,
                @ToolParam(name = "operation") String op,
                ToolEmitter emitter) {

            for (int i = 1; i <= 10; i++) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return "Processing interrupted at " + (i * 10) + "%";
                }
                // 通过 ToolEmitter 发射中间进度
                emitter.emit(ToolResultBlock.text("Progress: " + (i * 10) + "%"));
            }
            return "Done processing " + name;
        }
    }
}
