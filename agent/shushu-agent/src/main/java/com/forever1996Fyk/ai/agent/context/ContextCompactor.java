package com.forever1996Fyk.ai.agent.context;

import com.forever1996Fyk.ai.agent.prompts.ReactAgentPrompts;
import com.forever1996Fyk.ai.agent.token.TokenEstimator;
import com.forever1996Fyk.ai.agent.util.ThinkTagParser;
import org.apache.commons.collections4.CollectionUtils;
import org.bouncycastle.pqc.crypto.rainbow.Layer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/7/6 22:36
 **/
public class ContextCompactor {
    private static final Logger log = LoggerFactory.getLogger(ContextCompactor.class);
    private final ContextPolicy policy;
    private final ChatModel chatModel;

    public ContextCompactor(ContextPolicy policy, ChatModel chatModel) {
        this.policy = policy;
        this.chatModel = chatModel;
    }

    /**
     * 核心方法：压缩消息列表。直接修改传入的 messages 列表。
     *
     * @param messages        消息列表
     * @param currentQuestion 当前用户请求，用于引导摘要重点（可为 null）
     */
    public void compact(List<Message> messages, String currentQuestion) {
        if (messages == null || messages.size() <= 2) {
            return;
        }

        // Layer 1: micro_compact（每轮执行）
        microCompact(messages);

        //Layer 2: auto_compact（超阈值触发）
        int estimatedTokens = TokenEstimator.estimateTokens(messages);
        if (estimatedTokens > policy.tokenThreshold()) {
            log.info("Context auto_compact triggered: estimated tokens={} > threshold={}, messages={}",
                    estimatedTokens, policy.tokenThreshold(), messages.size());
            autoCompact(messages, currentQuestion);
        }
    }

    // ==================== Layer 1: micro_compact ====================

    /**
     * micro压缩的主要手段：
     * 1. 压缩工具返回结果
     * 2. 压缩大模型调用工具的参数
     * @param messages
     */
    private void microCompact(List<Message> messages) {
        // 1. 构建 toolCallId -> toolName 映射
        Map<String, String> toolNameMap = buildToolNameMap(messages);

        // 2. 收集 ToolResponseMessage 的索引
        List<Integer> trmIndices = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof ToolResponseMessage) {
                trmIndices.add(i);
            }
        }

        // 3. 收集包含 ToolCall 的 AssistantMessage 的索引
        List<Integer> assistantWithToolCallIndices = new ArrayList<>();
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i) instanceof AssistantMessage am
                    && CollectionUtils.isNotEmpty(am.getToolCalls())) {
                assistantWithToolCallIndices.add(i);
            }
        }

        // 4. 替换旧的 ToolResponse 内容（保留最近 keepRecentTools 个）
        int trmKeepCount = Math.min(policy.keepRecentTools(), trmIndices.size());
        int trmClearCount = trmIndices.size() - trmKeepCount;

        for (int idx = 0; idx < trmClearCount; idx++) {
            int msgIndex = trmIndices.get(idx);
            ToolResponseMessage original = (ToolResponseMessage) messages.get(msgIndex);

            List<ToolResponseMessage.ToolResponse> replaced = new ArrayList<>();
            for (var resp : original.getResponses()) {
                String content = resp.responseData();
                String toolName = resp.name();
                // 跳过受保护的工具
                if (policy.isProtected(toolName)) {
                    replaced.add(resp);
                    continue;
                }

                // 替换长内容为占位符（必须是合法 JSON 格式，否则模型报错）
                if (content.length() > policy.maxToolLength()) {
                    content = "{\"compacted\":true,\"tool\":\"" + toolName
                            + "\",\"originalLength\":" + content.length()
                            + ",\"message\":\"content compressed\"}";
                }
                replaced.add(new ToolResponseMessage.ToolResponse(resp.id(), resp.name(), content));
            }
            messages.set(msgIndex, ToolResponseMessage.builder().responses(replaced).build());
        }

        // 5. 替换旧的 AssistantMessage.ToolCall 长参数（保留最近 keepRecentTools 个）
        if (policy.maxToolLength() > 0) {
            int ascKeepCount = Math.min(policy.keepRecentTools(), assistantWithToolCallIndices.size());
            int ascClearCount = assistantWithToolCallIndices.size() - ascKeepCount;
            for (int idx = 0; idx < ascClearCount; idx++) {
                int msgIndex = assistantWithToolCallIndices.get(idx);
                AssistantMessage original = (AssistantMessage) messages.get(msgIndex);

                List<AssistantMessage.ToolCall> replaceCalls = new ArrayList<>();
                for (AssistantMessage.ToolCall tc : original.getToolCalls()) {
                    // 跳过受保护的工具
                    if (policy.isProtected(tc.name())) {
                        replaceCalls.add(tc);
                        continue;
                    }
                    String args = tc.arguments();
                    if (args.length() > policy.maxToolLength()) {
                        args = "{\"compacted\":true,\"tool\":\"" + tc.name()
                                + "\",\"originalLength\":" + args.length()
                                + ",\"message\":\"args compressed\"}";
                    }
                    replaceCalls.add(new AssistantMessage.ToolCall(
                            tc.id(), tc.type(), tc.name(), args));
                }
                messages.set(msgIndex, AssistantMessage.builder()
                        .content(original.getText())
                        .toolCalls(replaceCalls)
                        .build());
            }
        }
    }

    // ==================== Layer 2: auto_compact ====================

    /**
     * auto_compact：将所有旧消息（除 SystemMessage）交给 LLM 生成结构化摘要，然后替换。
     * 不保留任何旧消息、不提取 protected、不对齐边界。
     */
    private void autoCompact(List<Message> messages, String currentQuestion) {
        // 1. 保留 SystemMessage（第一条）
        SystemMessage systemMessage = null;
        if (!messages.isEmpty() && messages.getFirst() instanceof SystemMessage sm) {
            systemMessage = sm;
        }

        int systemMsgCount = systemMessage != null ? 1 : 0;

        // 2. 所有非 SystemMessage 的消息进入摘要
        List<Message> oldMessages = new ArrayList<>(
                messages.subList(systemMsgCount, messages.size()));

        // 3. LLM 生成结构化摘要（失败时回退到截断策略）
        String summary = generateSummary(oldMessages, currentQuestion);

        // 4. 重建消息列表：System → Summary → CurrentQuestion
        messages.clear();
        if (systemMessage != null) {
            messages.add(systemMessage);
        }

        messages.add(new UserMessage("[对话已压缩] 以下是之前对话的摘要：\n" + summary));
        log.info("Context compacted: {} old messages summarized into structured summary",
                oldMessages.size());
    }

    private String generateSummary(List<Message> messages, String currentQuestion) {
        String conversationText = buildConversationText(messages);

        try {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(ReactAgentPrompts.getCompactSummarySystemPrompt()),
                    new UserMessage(ReactAgentPrompts.getCompactSummaryUserPrompt(conversationText, currentQuestion))
            )));
            String summary = response.getResult().getOutput().getText();
            // 剥离 LLM 返回中的 think 内容
            summary = ThinkTagParser.stripThinkTags(summary);
            log.info("Context summary generated: {} chars input -> {} chars summary", conversationText.length(), summary != null ? summary.length() : 0);

            return summary != null ? summary : "";
        } catch (Exception e) {
            log.warn("LLM summary failed, falling back to message-level truncation: {}", e.getMessage());
            return truncationFallback(messages);
        }
    }

    /**
     * LLM 摘要失败时，回退到消息级截断。
     */
    private String truncationFallback(List<Message> messages) {
        int keepCount = 10;
        int start = Math.max(0, messages.size() - keepCount);
        StringBuilder sb = new StringBuilder();
        sb.append("...[摘要生成失败，以下为最近 ").append(keepCount).append(" 条对话内容]\n\n");
        for (int i = start; i < messages.size(); i++) {
            sb.append(extractMessageText(messages.get(i))).append("\n\n");
        }
        return sb.toString();
    }

    private String buildConversationText(List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for (Message msg : messages) {
            sb.append("[").append(msg.getMessageType()).append("] ");
            sb.append(extractMessageText(msg));
            sb.append("\n\n");
        }
        return sb.toString();
    }

    /**
     * 提取消息的文本内容（剥离 think 标签，ToolCall 保留名称和参数）。
     */
    private String extractMessageText(Message msg) {
        if (msg instanceof AssistantMessage am) {
            StringBuilder sb = new StringBuilder();
            String text = am.getText();
            sb.append(ThinkTagParser.stripThinkTags(text));
            if (CollectionUtils.isNotEmpty(am.getToolCalls())) {
                for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                    sb.append("\n[ToolCall: ").append(tc.name()).append(" args=").append(tc.arguments()).append("]");
                }
            }
            return sb.toString();
        } else if (msg instanceof ToolResponseMessage trm) {
            StringBuilder sb = new StringBuilder();
            for (var resp : trm.getResponses()) {
                sb.append(resp.responseData());
            }
            return sb.toString();
        } else {
            return msg.toString();
        }
    }

    private Map<String, String> buildToolNameMap(List<Message> messages) {
        Map<String, String> map = new HashMap<>();
        for (Message msg : messages) {
            if (msg instanceof AssistantMessage am && CollectionUtils.isNotEmpty(am.getToolCalls())) {
                for (AssistantMessage.ToolCall tc : am.getToolCalls()) {
                    map.put(tc.id(), tc.name());
                }
            }
        }
        return map;
    }
}
