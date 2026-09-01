package com.forever1996Fyk.ai.agentx.core.agent.internal;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * @program: AI-Learn
 * @description: buildInitialMessages 的返回值：消息列表 + 本次调用新增消息的起点。
 * @author: YuKai Fan
 * @create: 2026/9/1 09:27
 **/
public record BuiltMessages(List<Message> messages, int newMsgStartIndex) {
}
