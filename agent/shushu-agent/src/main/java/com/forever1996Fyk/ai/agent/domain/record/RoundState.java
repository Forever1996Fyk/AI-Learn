package com.forever1996Fyk.ai.agent.domain.record;

import com.forever1996Fyk.ai.agent.enums.RoundMode;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;

import java.util.List;

import static java.util.Collections.synchronizedList;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/20 17:46
 **/
@Data
public class RoundState {
    /**
     * 当前运行模式
     */
    public RoundMode mode = RoundMode.UNKNOWN;

    /**
     * 文本缓冲区
     */
    public StringBuilder textBuffer = new StringBuilder();

    /**
     * 工具调用列表
     */
    public List<AssistantMessage.ToolCall> toolCalls = synchronizedList(new java.util.ArrayList<>());

    /**
     * ThinkTagParser 的 inThink 状态，跨 chunk 追踪 <think/> 标签
     */
    public boolean inThink = false;
}
