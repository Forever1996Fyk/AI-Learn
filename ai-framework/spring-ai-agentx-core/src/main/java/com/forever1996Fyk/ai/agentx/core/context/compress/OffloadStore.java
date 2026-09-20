package com.forever1996Fyk.ai.agentx.core.context.compress;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 *
 * 被压缩消息的原文存储接口。
 * 被替换出的消息段按一个 UUID 整体存储，供 context_reload 工具回溯。
 *
 * 即：原始消息，在被压缩前，会把原始消息先存储，以供后续回溯
 *
 * @author: YuKai Fan
 * @create: 2026/9/18 11:05
 **/
public interface OffloadStore {

    /**
     * 将一段原始消息 offload，返回分配的 UUID。
     * conversationId 为 null 时直接返回 null（不持久化）。
     */
    String offload(String conversationId, long sessionId, List<Message> messages);

    default String offload(String conversationId, long sessionId, Message message) {
        return message == null ? null : offload(conversationId, sessionId, List.of(message));
    }

    /**
     * 按 UUID 取回原文消息段。不存在或不可用时返回空列表。
     */
    List<Message> load(String conversationId, String uuid);
}
