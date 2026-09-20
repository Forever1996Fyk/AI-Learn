package com.forever1996Fyk.ai.agentx.core.context.compress;

import com.forever1996Fyk.ai.agentx.core.context.ContextPolicy;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * 单次压缩调用的上下文。每轮 LLM 调用前由 ContextCompactor 构造一次。
 * 策略通过修改 messages 列表产生压缩效果，通过 offloadStore 持久化被替换出的原文。
 * @author: YuKai Fan
 * @create: 2026/9/18 11:09
 **/
public record CompressionContext(
        List<Message> messages,
        String query,
        String conversationId,
        long sessionId,
        ContextPolicy policy,
        ChatModel chatModel,
        OffloadStore offloadStore
) {

    /**
     * 历史轮次区域的扫描上界（exclusive）。
     * 受 lastKeep 保护：最末 lastKeep 条消息不进入历史扫描区。
     * 即：历史消息的前lastKeep条不会被压缩
     */
    public int historicalScanEnd(int lastKeep) {
        int latestUser = latestUserMsgIndex();
        if (latestUser < 0) {
            return Math.max(0, messages.size() - lastKeep);
        }
        int protectedStart = Math.max(0, messages.size() - lastKeep);
        return Math.min(latestUser, protectedStart);
    }

    /**
     * 最近一条 UserMessage 的索引，作为历史轮次与当前任务的分界。
     * 没有任何 UserMessage 时返回 -1。
     */
    public int latestUserMsgIndex() {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i) instanceof UserMessage) {
                return i;
            }
        }
        return -1;
    }

    public boolean hasOffloadStore() {
        return offloadStore != null && conversationId != null;
    }

}
