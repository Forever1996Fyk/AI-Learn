package com.forever1996Fyk.ai.intelligent.customer.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * @program: AI-Learn
 * @description:
 * @author: YuKai Fan
 * @create: 2026/6/6 00:06
 **/
public interface TitleSummaryService {

    @SystemMessage("你是一个对话标题生成助手。根据用户的第一句话，生成一个简洁的中文会话标题，要求：不超过20个字，不加引号，直接输出标题内容。")
    @UserMessage("请根据以下用户问题生成会话标题：{{it}}")
    String generateTitle(String userQuestion);
}
