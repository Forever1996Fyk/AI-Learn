package com.forever1996Fyk.ai.agentx.core.context.compress;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @program: AI-Learn
 * @description: 空实现：未启用 session 时使用，所有操作返回空结果。
 * @author: YuKai Fan
 * @create: 2026/9/18 11:06
 **/
public class NoOpOffloadStore implements OffloadStore {
    @Override
    public String offload(String conversationId, long sessionId, List<Message> messages) {
        return null;
    }

    @Override
    public List<Message> load(String conversationId, String uuid) {
        return List.of();
    }
}
