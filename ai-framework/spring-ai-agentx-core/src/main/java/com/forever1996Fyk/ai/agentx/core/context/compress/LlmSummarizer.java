package com.forever1996Fyk.ai.agentx.core.context.compress;

import com.forever1996Fyk.ai.agentx.core.stage.ThinkTagParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;

/**
 * @program: AI-Learn
 * @description:
 * LLM 摘要调用封装（L4/L5/L6 共享）。
 * 统一处理 think 标签剥离与失败回退。
 * @author: YuKai Fan
 * @create: 2026/9/18 11:11
 **/
public class LlmSummarizer {

    private static final Logger log = LoggerFactory.getLogger(LlmSummarizer.class);

    private final ChatModel chatModel;

    public LlmSummarizer(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * 调用 LLM 生成摘要。
     *
     * @param systemPrompt 系统提示词（定义摘要风格与约束）
     * @param userPrompt   用户提示词（含待压缩原文）
     * @return 摘要文本；失败返回 null
     */
    public String summarize(String systemPrompt, String userPrompt) {
        try {
            ChatResponse response = chatModel.call(new Prompt(List.of(
                    new SystemMessage(systemPrompt),
                    new UserMessage(userPrompt)
            )));
            String summary = response.getResult().getOutput().getText();
            summary = ThinkTagParser.stripThinkTags(summary);
            log.debug("[LlmSummarizer] summary generated: inputChars={}, outputChars={}",
                    userPrompt.length(),
                    summary != null ? summary.length() : 0);
            return summary;
        } catch (Exception e) {
            log.warn("[LlmSummarizer] LLM call failed: {}", e.getMessage());
            return null;
        }
    }
}
