package com.forever1996Fyk.ai.agentx.core.agent.internal;

import com.forever1996Fyk.ai.agentx.core.exception.AgentErrorCode;
import com.forever1996Fyk.ai.agentx.core.model.AgentStreamEvent;
import com.forever1996Fyk.ai.agentx.core.tools.toolsearch.DeferredToolRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/9/17 11:11
 **/
public class LlmInvoker {

    private static final Logger log = LoggerFactory.getLogger(LlmInvoker.class);

    static final long RETRY_INTERVAL_MS = 10000;

    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final int maxRetries;
    private final List<Advisor> advisors;
    private final List<ToolCallback> alwaysLoadTools;
    private final DeferredToolRegistry deferredToolRegistry;
    private final DeferredToolRegistry.Session deferredToolSession;

    public LlmInvoker(ChatClient chatClient, ChatModel chatModel,
                      int maxRetries, List<Advisor> advisors,
                      List<ToolCallback> alwaysLoadTools,
                      DeferredToolRegistry deferredToolRegistry,
                      DeferredToolRegistry.Session deferredToolSession) {
        this.chatClient = chatClient;
        this.chatModel = chatModel;
        this.maxRetries = maxRetries;
        this.advisors = advisors;
        this.alwaysLoadTools = alwaysLoadTools;
        this.deferredToolRegistry = deferredToolRegistry;
        this.deferredToolSession = deferredToolSession;
    }

    /**
     * 构建当前轮次可用的 ChatClient。
     * 当没有 deferredToolRegistry 时返回固定 ChatClient（向后兼容）；
     * 否则从当前 session 获取已发现工具，重建 ChatClient。
     */
    public ChatClient buildRoundChatClient() {
        if (deferredToolRegistry == null) {
            return chatClient;
        }

        List<ToolCallback> roundTools = new ArrayList<>();
        roundTools.addAll(alwaysLoadTools);
        roundTools.addAll(deferredToolSession.getActiveDeferredTools());
        roundTools.add(deferredToolSession.getToolSearchCallback());

        ChatClient.Builder clientBuilder = ChatClient.builder(chatModel);

        if (!advisors.isEmpty()) {
            clientBuilder.defaultAdvisors(advisors);
        }

        var toolOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(roundTools)
                .internalToolExecutionEnabled(false)
                .build();

        clientBuilder.defaultOptions(toolOptions);
        clientBuilder.defaultToolCallbacks(roundTools);

        return clientBuilder.build();
    }

    /**
     * 处理流式路径的重试/失败逻辑。
     * 统一 scheduleRound 和 forceFinalStream 中的 onErrorResume 重复代码。
     *
     * @param err                 异常
     * @param retryAttempt        当前重试次数
     * @param sink                事件接收器
     * @param retryAction         重试时执行的操作
     * @param logLabel            日志标识（如 "LLM stream error"）
     * @param onTerminalFailure   最终失败时（重试耗尽）的回调，在 sink.tryEmitComplete() 之前执行，
     *                            供调用方做终态落库（避免 doFinally 时序竞态）
     * @return Flux.empty() 供 onErrorResume 使用
     */
    public Flux<ChatResponse> handleStreamError(Throwable err, int retryAttempt,
                                                Sinks.Many<AgentStreamEvent> sink,
                                                Runnable retryAction, String logLabel,
                                                Runnable onTerminalFailure) {
        if (retryAttempt < maxRetries) {
            String apiDetail = extractHttpResponseBody(err);
            log.warn("{} (attempt {}/{}), retrying in {}ms: {}{}",
                    logLabel, retryAttempt + 1, maxRetries, RETRY_INTERVAL_MS, err.getMessage(),
                    apiDetail != null ? "\nAPI Response: " + apiDetail : "", err);
            String retryMsg = "LLM 调用失败，正在重试 (" + (retryAttempt + 1) + "/" + maxRetries + ")";
            sink.tryEmitNext(new AgentStreamEvent.Error(
                    AgentErrorCode.LLM_CALL_FAILED, retryMsg, err.getMessage()));
            Schedulers.boundedElastic().schedule(retryAction, RETRY_INTERVAL_MS, TimeUnit.MILLISECONDS);
        } else {
            String apiDetail = extractHttpResponseBody(err);
            log.error("{} failed after {} retries: {}{}",
                    logLabel, maxRetries, err.getMessage(),
                    apiDetail != null ? "\nAPI Response: " + apiDetail : "", err);
            // 在发射 Error/Complete 之前先做终态落库，避免 doFinally 时序竞态
            if (onTerminalFailure != null) {
                try {
                    onTerminalFailure.run();
                } catch (Exception e) {
                    log.warn("onTerminalFailure callback failed: {}", e.getMessage());
                }
            }
            String failMsg = "LLM 调用失败（已重试 " + maxRetries + " 次）";
            sink.tryEmitNext(new AgentStreamEvent.Error(
                    AgentErrorCode.LLM_CALL_FAILED, failMsg, err.getMessage()));
            sink.tryEmitNext(new AgentStreamEvent.Complete());
            sink.tryEmitComplete();
        }
        return Flux.empty();
    }

    /**
     * 从异常链中提取 HTTP 响应体。
     */
    private static String extractHttpResponseBody(Throwable err) {
        Throwable current = err;
        while (current != null) {
            if (current instanceof WebClientResponseException webErr) {
                String body = webErr.getResponseBodyAsString();
                if (StringUtils.isNotBlank(body)) {
                    return body;
                }
            }
            current = current.getCause();
        }
        return null;
    }
}
