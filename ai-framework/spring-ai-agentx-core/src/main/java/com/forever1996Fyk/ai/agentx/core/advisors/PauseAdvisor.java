package com.forever1996Fyk.ai.agentx.core.advisors;

import com.forever1996Fyk.ai.agentx.core.model.PendingToolCall;
import org.springframework.ai.chat.client.ChatClientMessageAggregator;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.lang.NonNull;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/8/31 17:43
 **/
public class PauseAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String PAUSE_REQUIRED = "pause.required";
    public static final String PENDING_TOOLS = "pause.pending.tools";


    /**
     * 审批工具集合：用户确认后由框架执行
     */
    private final Set<String> interceptToolNames;
    /**
     * 用户输入工具：用户回答即结果，不执行（最多一个）
     */
    private final String askUserToolName;

    public PauseAdvisor(Set<String> interceptToolNames) {
        this.interceptToolNames = interceptToolNames != null ? interceptToolNames : Set.of();
        this.askUserToolName = null;
    }

    /**
     * 构造函数：所有工具均为审批工具（向后兼容）。
     *
     * @param toolNames 拦截的工具名称
     */
    public PauseAdvisor(String... toolNames) {
        this.interceptToolNames = toolNames != null ? Set.of(toolNames) : Set.of();
        this.askUserToolName = null;
    }


    /**
     * 内部构造函数，供 Builder 使用。
     */
    private PauseAdvisor(Set<String> interceptToolNames, String askUserToolName) {
        this.interceptToolNames = interceptToolNames != null ? interceptToolNames : Set.of();
        this.askUserToolName = askUserToolName;
    }

    @Override
    public ChatClientResponse adviseCall(@NonNull ChatClientRequest request, @NonNull CallAdvisorChain chain) {
        ChatClientResponse response = chain.nextCall(request);

        if (response.chatResponse() == null || !response.chatResponse().hasToolCalls()) {
            return response;
        }
        List<PendingToolCall> pendingToolCalls = findPendingToolCalls(response.chatResponse());
        if (!pendingToolCalls.isEmpty()) {
            response.context().put(PAUSE_REQUIRED, true);
            response.context().put(PENDING_TOOLS, pendingToolCalls);
        }

        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(@NonNull ChatClientRequest request, @NonNull StreamAdvisorChain chain) {
        Flux<ChatClientResponse> responseFlux = chain.nextStream(request);

        AtomicReference<ChatClientResponse> aggregatedRef = new AtomicReference<>();
        return responseFlux.publish(shared -> {
            // 流式透传所有 chunk，同时聚合最终响应
            Flux<ChatClientResponse> streaming = new ChatClientMessageAggregator()
                    .aggregateChatClientResponse(shared, aggregatedRef::set);

            // 流结束后，检查聚合响应中是否有需要拦截的 tool call
            Flux<ChatClientResponse> pauseCheck = Flux.defer(() -> {
                ChatClientResponse aggregated = aggregatedRef.get();
                if (aggregated != null && aggregated.chatResponse() != null) {
                    List<PendingToolCall> pendingToolCalls = findPendingToolCalls(aggregated.chatResponse());
                    if (!pendingToolCalls.isEmpty()) {
                        aggregated.context().put(PAUSE_REQUIRED, true);
                        aggregated.context().put(PENDING_TOOLS, pendingToolCalls);
                        // 发射带 PAUSE_REQUIRED 标记的聚合响应，供下游读取 context
                        return Flux.just(aggregated);
                    }
                }
                return Flux.empty();
            });

            return streaming.concatWith(pauseCheck);
        });
    }

    @Override
    public String getName() {
        return "PauseAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private List<PendingToolCall> findPendingToolCalls(ChatResponse response) {
        List<PendingToolCall> pending = new ArrayList<>();
        for (AssistantMessage.ToolCall toolCall : response.getResult().getOutput().getToolCalls()) {
            if (shouldIntercept(toolCall.name())) {
                pending.add(new PendingToolCall(toolCall.id(), toolCall.name(), toolCall.arguments()));
            }
        }
        return pending;
    }

    /**
     * 判断指定工具是否需要拦截（包括审批工具和用户输入工具）。
     *
     * @param toolName 工具名称
     * @return 是否需要拦截
     */
    public boolean shouldIntercept(String toolName) {
        return interceptToolNames.contains(toolName) || toolName.equals(askUserToolName);
    }

    /**
     * 创建 Builder。
     *
     * @return Builder 实例
     */
    public static Builder builder() {
        return new Builder();
    }



    /**
     * Builder 模式构建 PauseAdvisor，支持区分审批工具和用户输入工具。
     */
    public static class Builder {
        private Set<String> approvalTools = Set.of();
        private String askUserToolName;

        /**
         * 配置审批工具名称。用户确认后由框架执行，用户拒绝则不执行。
         *
         * @param tools 审批工具名称
         * @return Builder
         */
        public Builder approvalTools(String... tools) {
            this.approvalTools = tools != null ? Set.of(tools) : Set.of();
            return this;
        }

        /**
         * 配置审批工具名称集合。
         *
         * @param tools 审批工具名称集合
         * @return Builder
         */
        public Builder approvalTools(Set<String> tools) {
            this.approvalTools = tools != null ? tools : Set.of();
            return this;
        }

        /**
         * 配置用户输入工具名称（最多一个）。
         * <p>
         * 用户输入工具的特征：用户回答即工具结果，不需要框架执行。
         * 用于 ask_user 或调用方自定义的输入类工具。
         *
         * @param toolName 用户输入工具名称
         * @return Builder
         */
        public Builder askUserTool(String toolName) {
            this.askUserToolName = toolName;
            return this;
        }

        /**
         * 构建 PauseAdvisor。
         *
         * @return PauseAdvisor 实例
         */
        public PauseAdvisor build() {
            return new PauseAdvisor(approvalTools, askUserToolName);
        }
    }
}
