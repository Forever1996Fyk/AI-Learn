package com.forever1996Fyk.ai.aiframework.agentscope.controller;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.DashScopeChatModel;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.transport.HttpTransportConfig;
import io.agentscope.core.model.transport.OkHttpTransport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Objects;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/26 22:52
 **/
@RestController
@RequestMapping("/retry")
public class RetryController {

    private final static String api_key = System.getenv("dashscope.api-key");

    @GetMapping("/chat")
    public String chat() {
        //  给模型调用设置更短的超时和更多重试
        ExecutionConfig modelConfig = ExecutionConfig.builder()
                // 单次请求超时30秒
                .timeout(Duration.ofSeconds(30))
                // 最多尝试5次（1次初始 + 4次重试）
                .maxAttempts(5)
                // 首次重试等1秒
                .initialBackoff(Duration.ofSeconds(1))
                // 退避上限15秒
                .maxBackoff(Duration.ofSeconds(15))
                // 指数退避：1s -> 2s -> 4s -> 8s -> 15s
                .backoffMultiplier(2.0)
                // / 使用默认可重试条件
                .retryOn(ExecutionConfig.RETRYABLE_ERRORS)
                .build();

        // 给工具调用设置更长的超时（某些工具耗时较长）
        ExecutionConfig toolConfig = ExecutionConfig.builder()
                // 工具执行最多等10分钟
                .timeout(Duration.ofMinutes(10))
                // 最多重试1次
                .maxAttempts(2)
                .initialBackoff(Duration.ofSeconds(3))
                // 仅网络错误时重试
                .retryOn(error -> error instanceof java.io.IOException)
                .build();

        HttpTransportConfig httpTransportConfig = HttpTransportConfig.builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofMinutes(3))
                .writeTimeout(Duration.ofSeconds(3))
                .build();

        // === 构建 Agent，分别指定模型和工具的执行配置 ===
        ReActAgent agent = ReActAgent.builder()
                .name("RobustAgent")
                .sysPrompt("You are a reliable assistant.")
                .model(DashScopeChatModel.builder()
                        .apiKey(api_key)
                        .modelName("qwen-plus")
                        .httpTransport(OkHttpTransport.builder()
                                .config(httpTransportConfig)
                                .build()
                        )
                        .build())
                // 模型调用的超时重试
                .modelExecutionConfig(modelConfig)
                // 工具调用的超时重试
                .toolExecutionConfig(toolConfig)
                .build();
        Msg msg = Msg.builder()
                .textContent("你是谁，现在几点了？")
                .build();

        return Objects.requireNonNull(agent.call(msg).block()).getTextContent();
    }
}
