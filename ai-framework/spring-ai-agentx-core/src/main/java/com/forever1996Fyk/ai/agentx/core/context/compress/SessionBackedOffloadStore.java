package com.forever1996Fyk.ai.agentx.core.context.compress;

import com.forever1996Fyk.ai.agentx.core.memory.store.SessionMessageStore;
import org.springframework.ai.chat.messages.Message;

import java.util.List;
import java.util.UUID;

/**
 * @program: AI-Learn
 * @description: 基于 SessionMessageStore 的 offload 实现，把原文写入 agentx_session.offload_context。
 * @author: YuKai Fan
 * @create: 2026/9/18 11:06
 **/
public record SessionBackedOffloadStore(SessionMessageStore sessionMessageStore) implements  OffloadStore {
    @Override
    public String offload(String conversationId, long sessionId, List<Message> messages) {
        if (conversationId == null || messages == null || messages.isEmpty() || sessionMessageStore == null) {
            return null;
        }
        String uuid = UUID.randomUUID().toString();
        sessionMessageStore.appendOffloadMessages(conversationId, sessionId, uuid, messages);
        return uuid;
    }

    @Override
    public List<Message> load(String conversationId, String uuid) {
        if (conversationId == null || uuid == null || sessionMessageStore == null) {
            return List.of();
        }
        return sessionMessageStore.getOffloaded(conversationId, uuid);
    }
}
